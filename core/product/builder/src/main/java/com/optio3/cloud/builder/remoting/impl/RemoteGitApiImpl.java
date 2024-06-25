/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.remoting.impl;

import static com.optio3.asyncawait.CompileTime.wrapAsync;
import static com.optio3.util.Exceptions.getAndUnwrapException;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3RemotableEndpoint;
import com.optio3.cloud.builder.model.jobs.input.CommitDetails;
import com.optio3.cloud.builder.model.jobs.input.CommitPerson;
import com.optio3.cloud.builder.remoting.RemoteGitApi;
import com.optio3.infra.GitHelper;
import com.optio3.infra.directory.UserInfo;
import com.optio3.util.Exceptions;
import com.optio3.util.function.FunctionWithException;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.lib.Ref;

@Optio3RemotableEndpoint(itf = RemoteGitApi.class)
public final class RemoteGitApiImpl implements RemoteGitApi
{
    @Override
    public CompletableFuture<Void> clone(String gitUrl,
                                         Path dbDir,
                                         Path dataDir,
                                         UserInfo credentials,
                                         FunctionWithException<String, CompletableFuture<Void>> progressCallback) throws
                                                                                                                  Exception
    {
        try (GitHelper gitHelper = getGitHelper(dbDir, dataDir, credentials))
        {
            if (progressCallback != null)
            {
                gitHelper.setProgressMonitor((text) ->
                                             {
                                                 // Unfortunately, we have to do a synchronous way, JGit is exposing only synchronous APIs.
                                                 getAndUnwrapException(progressCallback.apply(text));
                                             });
            }

            gitHelper.cloneRepository(gitUrl);

            return wrapAsync(null);
        }
    }

    @Override
    public CompletableFuture<List<CommitDetails>> describeBranches(String gitUrl,
                                                                   Path dbDir,
                                                                   Path dataDir,
                                                                   UserInfo credentials) throws
                                                                                         Exception
    {
        try (GitHelper gitHelper = getGitHelper(dbDir, dataDir, credentials))
        {
            gitHelper.fetch();

            List<CommitDetails> res = Lists.newArrayList();

            for (Ref ref : gitHelper.listRemoteBranches())
            {
                GitHelper.CommitInfo ci = gitHelper.describeCommit(ref.getObjectId()
                                                                      .getName());
                CommitDetails cd = toCommitDetails(ci);
                cd.branch = gitHelper.getRemoteBranchName(ref);

                res.add(cd);
            }

            return wrapAsync(res);
        }
    }

    @Override
    public CompletableFuture<CommitDetails> switchToBranch(String gitUrl,
                                                           Path dbDir,
                                                           Path dataDir,
                                                           UserInfo credentials,
                                                           String branch,
                                                           String commit,
                                                           FunctionWithException<String, CompletableFuture<Void>> progressCallback) throws
                                                                                                                                    Exception
    {
        try (GitHelper gitHelper = getGitHelper(dbDir, dataDir, credentials))
        {
            if (progressCallback != null)
            {
                gitHelper.setProgressMonitor((text) ->
                                             {
                                                 // Unfortunately, we have to do a synchronous way, JGit is exposing only synchronous APIs.
                                                 getAndUnwrapException(progressCallback.apply(text));
                                             });
            }

            gitHelper.fetch();

            Ref ref = gitHelper.findRemoteBranch(branch);
            if (ref == null)
            {
                throw Exceptions.newRuntimeException("No branch '%s' in Repo '%s'", branch, gitUrl);
            }

            String actualBranch = gitHelper.switchToBranch(ref);

            gitHelper.resetHard();

            gitHelper.pull(true);

            // After the pull, we can try to select the desired commit.
            if (commit != null)
            {
                gitHelper.resetHardToCommit(commit);
            }

            GitHelper.CommitInfo ci = gitHelper.describeCommit(commit);
            CommitDetails        cd = toCommitDetails(ci);
            cd.branch = actualBranch;

            return wrapAsync(cd);
        }
    }

    @Override
    public CompletableFuture<Void> enumerateCommits(Path dbDir,
                                                    Path dataDir,
                                                    String head,
                                                    int skip,
                                                    int maxCount,
                                                    int batchSize,
                                                    FunctionWithException<List<CommitDetails>, CompletableFuture<Boolean>> callback) throws
                                                                                                                                     Exception
    {
        try (GitHelper gitHelper = getGitHelper(dbDir, dataDir, null))
        {
            List<CommitDetails> batch = Lists.newArrayList();

            gitHelper.listCommits(head, skip, maxCount, (ci) ->
            {
                batch.add(toCommitDetails(ci));

                flush(batch, batchSize, callback);
            });

            flush(batch, 0, callback);

            return wrapAsync(null);
        }
    }

    private boolean flush(List<CommitDetails> batch,
                          int batchSize,
                          FunctionWithException<List<CommitDetails>, CompletableFuture<Boolean>> callback) throws
                                                                                                           Exception
    {
        if (batch.size() <= batchSize)
        {
            return true;
        }

        boolean res = getAndUnwrapException(callback.apply(batch)) == Boolean.TRUE;

        batch.clear();

        return res;
    }

    @Override
    public CompletableFuture<Void> reset(Path dbDir,
                                         Path dataDir) throws
                                                       Exception
    {
        try (GitHelper gitHelper = getGitHelper(dbDir, dataDir, null))
        {
            gitHelper.reset(ResetType.HARD);

            return wrapAsync(null);
        }
    }

    @Override
    public CompletableFuture<Set<String>> clean(Path dbDir,
                                                Path dataDir,
                                                boolean dryRun) throws
                                                                Exception
    {
        try (GitHelper gitHelper = getGitHelper(dbDir, dataDir, null))
        {
            Set<String> res = gitHelper.clean(dryRun);

            return wrapAsync(res);
        }
    }

    //--//

    private GitHelper getGitHelper(Path dbDir,
                                   Path dataDir,
                                   UserInfo credentials)
    {
        GitHelper gitHelper = new GitHelper(dataDir, dbDir);
        if (credentials != null)
        {
            gitHelper.setCredentials(credentials);
        }

        return gitHelper;
    }

    private CommitDetails toCommitDetails(GitHelper.CommitInfo ci)
    {
        CommitDetails cd = new CommitDetails();

        cd.id = ci.id;
        cd.message = ci.message;
        cd.author = toCommitPerson(ci.author);
        cd.committer = toCommitPerson(ci.committer);
        cd.parents = ci.parents;

        return cd;
    }

    private static CommitPerson toCommitPerson(GitHelper.CommitPerson cpOther)
    {
        CommitPerson cp = new CommitPerson();

        cp.name = cpOther.name;
        cp.emailAddress = cpOther.emailAddress;
        cp.when = cpOther.when;

        return cp;
    }
}
