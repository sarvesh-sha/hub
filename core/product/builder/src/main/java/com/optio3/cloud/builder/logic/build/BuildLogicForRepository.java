/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.logic.build;

import static com.optio3.util.Exceptions.getAndUnwrapException;
import static java.util.Objects.requireNonNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.NotFoundException;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.model.jobs.input.CommitDetails;
import com.optio3.cloud.builder.model.jobs.input.RepositoryRefresh;
import com.optio3.cloud.builder.persistence.jobs.JobRecord;
import com.optio3.cloud.builder.persistence.jobs.JobStepRecord;
import com.optio3.cloud.builder.persistence.jobs.input.RepositoryBranchRecord;
import com.optio3.cloud.builder.persistence.jobs.input.RepositoryCommitRecord;
import com.optio3.cloud.builder.persistence.jobs.input.RepositoryRecord;
import com.optio3.cloud.builder.persistence.worker.HostRecord;
import com.optio3.cloud.builder.persistence.worker.ManagedDirectoryRecord;
import com.optio3.cloud.builder.persistence.worker.RepositoryCheckoutRecord;
import com.optio3.cloud.builder.remoting.RemoteFileSystemApi;
import com.optio3.cloud.builder.remoting.RemoteGitApi;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.LogHolder;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.infra.directory.RoleType;
import com.optio3.infra.directory.UserInfo;
import com.optio3.util.Exceptions;
import com.optio3.util.IdGenerator;

public class BuildLogicForRepository extends BaseBuildLogicWithJob
{
    public static final String DIR_FOR_DB          = ".git";
    public static final String DIR_FOR_WORKINGTREE = ".data";

    //--//

    //--//

    private final SessionHolder m_sessionHolder;

    public BuildLogicForRepository(BuilderConfiguration config,
                                   SessionHolder sessionHolder,
                                   HostRecord targetHost,
                                   JobRecord job)
    {
        super(config, targetHost, job);

        m_sessionHolder = sessionHolder;
    }

    /**
     * Acquires a checkout for the target repository.
     * <p>
     * If an existing checkout is available (same repository and not in use by another job), it will acquire it, and return it.
     * <br>
     * Otherwise a new checkout gets provisioned on the target host.
     *
     * @param lock_step Job Step for capturing the output
     * @param repo      Target repository
     *
     * @return Checkout available for the job
     *
     * @throws Exception
     */
    public RepositoryCheckoutRecord acquire(RecordLocked<JobStepRecord> lock_step,
                                            RepositoryRecord repo) throws
                                                                   Exception
    {
        requireNonNull(repo);

        final RecordHelper<RepositoryCheckoutRecord> helper = m_sessionHolder.createHelper(RepositoryCheckoutRecord.class);

        helper.lockTableUntilEndOfTransaction(1, TimeUnit.MINUTES);

        for (RepositoryCheckoutRecord rec_checkout : Lists.newArrayList(repo.getCheckouts()))
        {
            if (rec_checkout.getOwningHost() == m_targetHost && rec_checkout.tryToAcquire(helper, m_job))
            {
                try
                {
                    RemoteGitApi proxy = getProxy(RemoteGitApi.class);

                    Path dbDir = rec_checkout.getDirectoryForDb()
                                             .getPath();
                    Path dataDir = rec_checkout.getDirectoryForWork()
                                               .getPath();

                    getAndUnwrapException(proxy.reset(dbDir, dataDir));
                    getAndUnwrapException(proxy.clean(dbDir, dataDir, false));
                }
                catch (Throwable t)
                {
                    // If anything goes wrong with the checkout, just delete it.
                    try (ValidationResultsHolder validation = new ValidationResultsHolder(m_sessionHolder, null, true))
                    {
                        rec_checkout.deleteRecursively(m_config.hostRemoter, validation);
                    }
                    continue;
                }

                return rec_checkout;
            }
        }

        return newCheckout(lock_step, repo);
    }

    public RepositoryCheckoutRecord newCheckout(RecordLocked<JobStepRecord> lock_step,
                                                RepositoryRecord repo) throws
                                                                       Exception
    {
        String prefix  = IdGenerator.newGuid();
        Path   root    = Paths.get(m_config.managedDirectoriesRoot);
        Path   dbDir   = root.resolve(prefix + DIR_FOR_DB);
        Path   dataDir = root.resolve(prefix + DIR_FOR_WORKINGTREE);

        ManagedDirectoryRecord rec_dirForDb = ManagedDirectoryRecord.newInstance(m_targetHost, dbDir);
        m_sessionHolder.persistEntity(rec_dirForDb);

        ManagedDirectoryRecord rec_dirForWork = ManagedDirectoryRecord.newInstance(m_targetHost, dataDir);
        m_sessionHolder.persistEntity(rec_dirForWork);

        RepositoryCheckoutRecord rec_newCheckout = RepositoryCheckoutRecord.newInstance(m_targetHost, repo);
        rec_newCheckout.setDirectoryForDb(rec_dirForDb);
        rec_newCheckout.setDirectoryForWork(rec_dirForWork);
        m_sessionHolder.persistEntity(rec_newCheckout);
        rec_newCheckout.acquire(m_job);
        m_sessionHolder.flush();

        //
        // Since we are about to create local directories,
        // register for transaction notification to delete the data from disk,
        // in case the records for the managed directories cannot be committed to the database.
        //
        m_sessionHolder.onTransactionRollback(() ->
                                              {
                                                  try (ValidationResultsHolder validation = new ValidationResultsHolder(m_sessionHolder, null, true))
                                                  {
                                                      rec_dirForDb.freeResources(m_config.hostRemoter, validation);
                                                      rec_dirForWork.freeResources(m_config.hostRemoter, validation);
                                                  }
                                              });

        {
            RemoteGitApi proxy = getProxy(RemoteGitApi.class);

            UserInfo user = getCredentialForRepo(repo);

            try (var logHandler = JobStepRecord.allocateLogHandler(lock_step))
            {
                try (LogHolder logHolder = logHandler.newLogHolder())
                {
                    getAndUnwrapException(proxy.clone(repo.getGitUrl(), dbDir, dataDir, user, logHolder::addTextAsync));
                }
            }
        }

        return rec_newCheckout;
    }

    /**
     * Releases the checkout back to the pool of available repository checkouts.
     *
     * @param checkout The checkout to release
     *
     * @throws Exception
     */
    public void release(RepositoryCheckoutRecord checkout) throws
                                                           Exception
    {
        ensureAcquired(checkout);

        checkout.release(m_config.hostRemoter, m_sessionHolder);

        m_sessionHolder.flush();
    }

    /**
     * Lists all the remote branches.
     *
     * @param checkout Target checkout
     *
     * @throws Exception
     */
    public List<CommitDetails> listBranches(RepositoryCheckoutRecord checkout) throws
                                                                               Exception
    {
        ensureAcquired(checkout);

        RemoteGitApi     proxy = getProxy(RemoteGitApi.class);
        RepositoryRecord repo  = checkout.getRepository();
        UserInfo         user  = getCredentialForRepo(repo);

        Path dbDir = checkout.getDirectoryForDb()
                             .getPath();

        Path dataDir = checkout.getDirectoryForWork()
                               .getPath();

        CommitDetails head;

        return getAndUnwrapException(proxy.describeBranches(repo.getGitUrl(), dbDir, dataDir, user));
    }

    /**
     * Switches the target checkout to the desired branch, optionally selecting a specific commit.
     *
     * @param lock_step Job Step for capturing the output
     * @param checkout  Target checkout
     * @param branch    Desired branch
     * @param commit    Optionally, a commit target
     *
     * @throws Exception
     */
    public CommitDetails switchToBranch(RecordLocked<JobStepRecord> lock_step,
                                        RepositoryCheckoutRecord checkout,
                                        String branch,
                                        String commit) throws
                                                       Exception
    {
        ensureAcquired(checkout);

        RemoteGitApi     proxy = getProxy(RemoteGitApi.class);
        RepositoryRecord repo  = checkout.getRepository();
        UserInfo         user  = getCredentialForRepo(repo);

        Path dbDir = checkout.getDirectoryForDb()
                             .getPath();

        Path dataDir = checkout.getDirectoryForWork()
                               .getPath();

        CommitDetails head;

        try (var logHandler = JobStepRecord.allocateLogHandler(lock_step))
        {
            try (LogHolder logHolder = logHandler.newLogHolder())
            {
                head = getAndUnwrapException(proxy.switchToBranch(repo.getGitUrl(), dbDir, dataDir, user, branch, commit, logHolder::addTextAsync));
                checkout.setCurrentBranch(head.branch);
                checkout.setCurrentCommit(head.id);
            }
        }

        RepositoryRefresh status = new RepositoryRefresh();
        synchronizeRepositoryRecords(checkout, status);

        return head;
    }

    /**
     * Walks through the log of the target checkout and updates the database with branch and commit records.
     *
     * @param checkout Target checkout
     * @param status
     *
     * @throws Exception
     */
    public void synchronizeRepositoryRecords(RepositoryCheckoutRecord checkout,
                                             RepositoryRefresh status) throws
                                                                       Exception
    {
        ensureAcquired(checkout);

        RemoteGitApi     proxy = getProxy(RemoteGitApi.class);
        RepositoryRecord repo  = checkout.getRepository();
        UserInfo         user  = getCredentialForRepo(repo);

        Path dbDir = checkout.getDirectoryForDb()
                             .getPath();
        Path dataDir = checkout.getDirectoryForWork()
                               .getPath();

        RecordHelper<RepositoryRecord>       helperRepo   = m_sessionHolder.createHelper(RepositoryRecord.class);
        RecordHelper<RepositoryCommitRecord> helperCommit = m_sessionHolder.createHelper(RepositoryCommitRecord.class);
        RecordHelper<RepositoryBranchRecord> helperBranch = m_sessionHolder.createHelper(RepositoryBranchRecord.class);

        //
        // Make sure we don't race with another thread.
        //
        helperRepo.lockTableUntilEndOfTransaction(30, TimeUnit.MINUTES);

        Set<String> activeBranches = Sets.newHashSet();

        for (CommitDetails head : getAndUnwrapException(proxy.describeBranches(repo.getGitUrl(), dbDir, dataDir, user)))
        {
            //
            // First, collect all the hash values already in the database.
            //
            Set<String> commitHashes = Sets.newHashSet(repo.getCommits(helperCommit));

            //
            // Then walk the commits, stopping at the first one ALREADY in the database.
            // Commits are immutable, if we find one, it means we already processed its parents
            //
            List<CommitDetails> commits = Lists.newArrayList();
            getAndUnwrapException(proxy.enumerateCommits(dbDir, dataDir, head.id, -1, -1, 100, (cds) ->
            {
                for (CommitDetails cd : cds)
                {
                    String val = repo.fromCommitHashToSysId(cd.id);

                    if (commitHashes.contains(val))
                    {
                        return AsyncRuntime.False;
                    }

                    commits.add(cd);
                }

                return AsyncRuntime.True;
            }));

            //
            // Create the records for the missing commits.
            //
            for (CommitDetails cd : commits)
            {
                RepositoryCommitRecord rec = RepositoryCommitRecord.newInstance(repo, cd.id, cd.author.when);
                rec.setAuthorName(cd.author.name);
                rec.setAuthorEmailAddress(cd.author.emailAddress);
                rec.setMessage(cd.message);
                rec.setParents(cd.parents);

                helperCommit.persist(rec);

                status.commitsAdded++;
            }

            //
            // Finally, point the Branch record to the head commit.
            //
            if (head.branch != null)
            {
                activeBranches.add(head.branch);

                RepositoryBranchRecord rec_branch = repo.findBranch(head.branch);
                if (rec_branch == null)
                {
                    rec_branch = RepositoryBranchRecord.newInstance(repo, head.branch);

                    helperBranch.persist(rec_branch);

                    status.branchesAdded++;
                }

                RepositoryCommitRecord rec_commit = repo.findCommitByHash(helperCommit, head.id);
                if (rec_commit != null)
                {
                    rec_branch.setHead(rec_commit);
                }
            }
        }

        for (RepositoryBranchRecord rec_branch : Lists.newArrayList(repo.getBranches()))
        {
            if (!activeBranches.contains(rec_branch.getName()))
            {
                helperBranch.delete(rec_branch);
            }
        }
    }

    /**
     * Causes the checkout to be reset to the current commit, hard.
     *
     * @param checkout Target checkout
     *
     * @throws Exception
     */
    public void reset(RepositoryCheckoutRecord checkout) throws
                                                         Exception
    {
        ensureAcquired(checkout);

        RemoteGitApi proxy = getProxy(RemoteGitApi.class);

        Path dbDir = checkout.getDirectoryForDb()
                             .getPath();
        Path dataDir = checkout.getDirectoryForWork()
                               .getPath();

        getAndUnwrapException(proxy.reset(dbDir, dataDir));
    }

    /**
     * Causes all the files not tracked by the current branch to be deleted.
     *
     * @param checkout Target checkout
     * @param dryRun   If true, it would simply return the list of files and directories that would be deleted
     *
     * @return The files and directories that were processed
     *
     * @throws Exception
     */
    public Set<String> clean(RepositoryCheckoutRecord checkout,
                             boolean dryRun) throws
                                             Exception
    {
        ensureAcquired(checkout);

        RemoteGitApi proxy = getProxy(RemoteGitApi.class);

        Path dbDir = checkout.getDirectoryForDb()
                             .getPath();
        Path dataDir = checkout.getDirectoryForWork()
                               .getPath();

        return getAndUnwrapException(proxy.clean(dbDir, dataDir, dryRun));
    }

    /**
     * Reads a file from a remote repository.
     *
     * @param workDir      The root of the repository
     * @param relativePath The relative path of the file.
     *
     * @return the contents of the file or NULL if the file doesn't exist.
     *
     * @throws Exception
     */
    public byte[] readFile(ManagedDirectoryRecord workDir,
                           Path relativePath) throws
                                              Exception
    {
        ensureAcquired(workDir);

        RemoteFileSystemApi proxy = getProxy(RemoteFileSystemApi.class);

        Path dir  = workDir.getPath();
        Path file = dir.resolve(relativePath);

        return getAndUnwrapException(proxy.readFile(file, 0, -1));
    }

    //--//

    /**
     * Given a repository, it returns the account to use for interacting with it.
     *
     * @param repo Target repository
     *
     * @return Credentials for the repository
     */
    public UserInfo getCredentialForRepo(RepositoryRecord repo)
    {
        requireNonNull(repo);

        String gitUrl = repo.getGitUrl();

        if (!gitUrl.contains(":"))
        {
            // Not a URL, it must be a local Git repository, no need to credentials
            return null;
        }

        String host;

        try
        {
            host = new URL(gitUrl).getHost();
        }
        catch (MalformedURLException e)
        {
            throw new RuntimeException(e);
        }

        try
        {
            return getCredentialForHost(host, RoleType.Automation);
        }
        catch (NotFoundException e)
        {
            throw Exceptions.newRuntimeException("No credentials for Repo %s", gitUrl);
        }
    }
}
