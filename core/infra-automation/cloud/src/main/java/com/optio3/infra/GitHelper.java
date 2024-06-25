/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.Lists;
import com.optio3.infra.directory.UserInfo;
import com.optio3.logging.Logger;
import com.optio3.util.Exceptions;
import com.optio3.util.function.ConsumerWithException;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.CleanCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.RebaseCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.AndRevFilter;
import org.eclipse.jgit.revwalk.filter.MaxCountRevFilter;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.revwalk.filter.SkipRevFilter;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

public class GitHelper implements AutoCloseable
{
    public static class CommitPerson
    {
        public String        name;
        public String        emailAddress;
        public ZonedDateTime when;

        public LocalDateTime toLocalWhen()
        {
            return when.withZoneSameInstant(ZoneId.systemDefault())
                       .toLocalDateTime();
        }
    }

    public static class CommitInfo
    {
        public String   id;
        public String[] parents;

        public String       message;
        public CommitPerson author;
        public CommitPerson committer;
    }

    public static final Logger LoggerInstance = new Logger(GitHelper.class);

    private final Path m_gitDir;
    private final Path m_workTree;

    private CredentialsProvider m_credentialsProvider;

    private Repository m_repository;
    private Git        m_git;

    private ProgressMonitor m_progressMonitor;

    public GitHelper(Path workTree)
    {
        this(workTree, null);
    }

    public GitHelper(Path workTree,
                     Path gitDir)
    {
        m_workTree = workTree;
        m_gitDir = gitDir;
    }

    @Override
    public void close()
    {
        if (m_git != null)
        {
            m_git.close();
            m_git = null;
        }

        if (m_repository != null)
        {
            m_repository.close();
            m_repository = null;
        }
    }

    //--//

    public void setProgressMonitor(ConsumerWithException<String> callback)
    {
        Writer out = new StringWriter()
        {
            @Override
            public void write(String str)
            {
                try
                {
                    callback.accept(str);
                }
                catch (Exception e)
                {
                    LoggerInstance.error("setProgressMonitor encountered a problem: %s", e);
                }
            }
        };

        m_progressMonitor = new TextProgressMonitor(out);
    }

    public Git getGit()
    {
        Objects.requireNonNull(m_git);

        return m_git;
    }

    public GitHelper setCredentials(UserInfo ui)
    {
        m_credentialsProvider = new UsernamePasswordCredentialsProvider(ui.user, ui.getEffectivePassword());

        return this;
    }

    public void closeRepository()
    {
        m_git = null;
    }

    public Git openRepository() throws
                                IOException
    {
        if (m_git == null)
        {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            builder.setWorkTree(m_workTree.toFile());
            builder.setGitDir(getEffectiveGitDir().toFile());
            builder.setMustExist(true);

            m_repository = builder.build();
            m_git = new Git(m_repository);
        }

        return m_git;
    }

    public Git cloneRepository(String repo) throws
                                            GitAPIException,
                                            IOException
    {
        File dataDir = m_workTree.toFile();
        File gitDir  = getEffectiveGitDir().toFile();

        CloneCommand cmd = Git.cloneRepository();
        cmd.setURI(repo);
        cmd.setDirectory(dataDir);
        cmd.setGitDir(gitDir);
        cmd.setNoCheckout(true);
        cmd.setProgressMonitor(m_progressMonitor);

        m_git = callWithCredentials(cmd);

        //
        // Make sure we can set permissions.
        //
        StoredConfig config = m_git.getRepository()
                                   .getConfig();
        config.setBoolean(ConfigConstants.CONFIG_CORE_SECTION, null, ConfigConstants.CONFIG_KEY_FILEMODE, true);
        config.save();

        //
        // After configuring permissions properly, do the checkout.
        //
        Ref ref = findRemoteBranch(Constants.MASTER);
        if (ref == null)
        {
            throw Exceptions.newRuntimeException("No branch '%s' in Repo '%s'", Constants.MASTER, repo);
        }

        switchToBranch(ref);

        return m_git;
    }

    public Set<String> clean(boolean dryRun) throws
                                             IOException,
                                             GitAPIException,
                                             NoWorkTreeException
    {
        Git git = openRepository();

        CleanCommand cmd = git.clean();
        cmd.setDryRun(dryRun);
        cmd.setCleanDirectories(true);
        cmd.setIgnore(false);

        return cmd.call();
    }

    public void reset(ResetType mode) throws
                                      IOException,
                                      GitAPIException
    {
        Git git = openRepository();

        ResetCommand cmd = git.reset();
        cmd.setMode(mode);

        cmd.call();
    }

    public void fetch() throws
                        IOException,
                        GitAPIException
    {
        Git git = openRepository();

        FetchCommand cmd = git.fetch();

        cmd.setProgressMonitor(m_progressMonitor);
        cmd.setRemoveDeletedRefs(true);

        callWithCredentials(cmd);
    }

    public void pull(boolean useRebase) throws
                                        IOException,
                                        GitAPIException
    {
        Git git = openRepository();

        PullCommand cmd = git.pull();
        cmd.setRebase(useRebase);

        cmd.setProgressMonitor(m_progressMonitor);

        callWithCredentials(cmd);
    }

    public List<Ref> listRemoteBranches() throws
                                          GitAPIException
    {
        List<Ref> refs = m_git.branchList()
                              .setListMode(ListMode.REMOTE)
                              .call();

        return refs;
    }

    public String getRemoteBranchName(Ref ref)
    {
        Repository repository = m_git.getRepository();
        return repository.shortenRemoteBranchName(ref.getName());
    }

    public Ref findRemoteBranch(String targetBranch) throws
                                                     GitAPIException
    {
        for (Ref ref : listRemoteBranches())
        {
            if (ref.isSymbolic())
            {
                continue;
            }

            String branchName = getRemoteBranchName(ref);
            if (StringUtils.equals(branchName, targetBranch))
            {
                return ref;
            }
        }

        return null;
    }

    public String switchToBranch(Ref ref) throws
                                          RefNotFoundException,
                                          InvalidRefNameException,
                                          GitAPIException
    {
        Repository repository       = m_git.getRepository();
        String     branchName       = repository.shortenRemoteBranchName(ref.getName());
        String     remoteBranchName = Repository.shortenRefName(ref.getName());

        try
        {
            CreateBranchCommand cmd = m_git.branchCreate();
            cmd.setName(branchName);
            cmd.setUpstreamMode(SetupUpstreamMode.TRACK);
            cmd.setStartPoint(remoteBranchName);
            cmd.call();
        }
        catch (RefAlreadyExistsException e)
        {
        }

        m_git.checkout()
             .setName(branchName)
             .setForceRefUpdate(true)
             .setForced(true)
             .call();

        return branchName;
    }

    public void resetHard() throws
                            GitAPIException
    {
        m_git.reset()
             .setMode(ResetType.HARD)
             .call();

        try
        {
            m_git.rebase()
                 .setOperation(RebaseCommand.Operation.ABORT)
                 .call();
        }
        catch (Exception e)
        {
            // Swallow
        }
    }

    public void resetHardToCommit(String commit) throws
                                                 GitAPIException
    {
        m_git.reset()
             .setMode(ResetType.HARD)
             .setRef(commit)
             .call();
    }

    public void listCommits(String revision,
                            int skip,
                            int maxCount,
                            ConsumerWithException<CommitInfo> callback) throws
                                                                        MissingObjectException,
                                                                        IncorrectObjectTypeException,
                                                                        IOException
    {
        Git        git        = openRepository();
        Repository repository = git.getRepository();

        if (revision == null)
        {
            revision = Constants.HEAD;
        }

        ObjectId headId = repository.resolve(revision);

        try (RevWalk revWalk = new RevWalk(repository))
        {
            revWalk.markStart(revWalk.lookupCommit(headId));

            List<RevFilter> filters = Lists.newArrayList();
            if (skip > -1)
            {
                filters.add(SkipRevFilter.create(skip));
            }

            if (maxCount > -1)
            {
                filters.add(MaxCountRevFilter.create(maxCount));
            }

            switch (filters.size())
            {
                case 0:
                    break;

                case 1:
                    revWalk.setRevFilter(filters.get(0));
                    break;

                default:
                    revWalk.setRevFilter(AndRevFilter.create(filters));
                    break;
            }

            for (RevCommit commit : revWalk)
            {
                try
                {
                    CommitInfo ci = toCommitInfo(commit);
                    callback.accept(ci);
                }
                catch (Exception e)
                {
                    LoggerInstance.error("listCommits encountered a problem: %s", e);
                }
            }
        }
    }

    public CommitInfo describeCommit(String revstr) throws
                                                    MissingObjectException,
                                                    IncorrectObjectTypeException,
                                                    IOException
    {
        Git        git        = openRepository();
        Repository repository = git.getRepository();

        try (RevWalk revWalk = new RevWalk(repository))
        {
            RevCommit commit = revWalk.parseCommit(repository.resolve(revstr == null ? Constants.HEAD : revstr));

            return toCommitInfo(commit);
        }
    }

    private static CommitInfo toCommitInfo(RevCommit commit)
    {
        CommitInfo ci = new CommitInfo();

        ci.id = commit.getId()
                      .getName();
        ci.message = commit.getFullMessage();
        ci.author = toPerson(commit.getAuthorIdent());
        ci.committer = toPerson(commit.getCommitterIdent());

        RevCommit[] parents = commit.getParents();
        if (parents != null && parents.length > 0)
        {
            ci.parents = new String[parents.length];
            for (int i = 0; i < parents.length; i++)
                ci.parents[i] = parents[i].getId()
                                          .getName();
        }

        return ci;
    }

    private static CommitPerson toPerson(PersonIdent ident)
    {
        CommitPerson cp = new CommitPerson();

        cp.name = ident.getName();
        cp.emailAddress = ident.getEmailAddress();

        ZoneOffset zoneOffset = ZoneOffset.ofTotalSeconds(ident.getTimeZoneOffset() * 60);
        ZoneId     zoneId     = ZoneId.from(zoneOffset);

        Instant when = ident.getWhen()
                            .toInstant();
        cp.when = when.atZone(zoneId);

        return cp;
    }

    //--//

    private <C extends TransportCommand<C, T>, T> T callWithCredentials(C cmd) throws
                                                                               GitAPIException
    {
        if (m_credentialsProvider != null)
        {
            cmd.setCredentialsProvider(m_credentialsProvider);
        }

        return cmd.call();
    }

    private Path getEffectiveGitDir()
    {
        Objects.requireNonNull(m_workTree);

        return m_gitDir != null ? m_gitDir : m_workTree.resolve(".git");
    }
}
