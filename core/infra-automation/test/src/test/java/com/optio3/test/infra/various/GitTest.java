/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.infra.various;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import com.optio3.infra.GitHelper;
import com.optio3.infra.GitHubHelper;
import com.optio3.infra.directory.RoleType;
import com.optio3.infra.directory.UserInfo;
import com.optio3.test.infra.Optio3InfraTest;
import com.optio3.util.FileSystem;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class GitTest extends Optio3InfraTest
{
    @Before
    public void setup() throws
                        Exception
    {
        ensureCredentials(false, false);
    }

    @Ignore("Manually enable to test, since it requires access to local repositories.")
    @Test
    public void testJGit() throws
                           IOException,
                           GitAPIException
    {
        Path repoDir = Paths.get(System.getenv("HOME"), "git/third_party/jersey");

        try (GitHelper gitHelper = new GitHelper(repoDir))
        {
            Git        git        = gitHelper.openRepository();
            Repository repository = git.getRepository();

            System.out.println("Having repository: " + repository.getDirectory());

            // the Ref holds an ObjectId for any type of object (tree, commit, blob, tree)
            Ref head = repository.exactRef("refs/heads/master");
            System.out.println("Ref of refs/heads/master: " + head);

            List<Ref> refs = git.branchList()
                                .setListMode(ListMode.REMOTE)
                                .call();

            for (Ref ref : refs)
            {
                if (ref.isSymbolic())
                {
                    continue;
                }

                System.out.println("Branch: " + ref + " " + ref.getName() + " " + ref.getObjectId()
                                                                                     .getName());

                String branchName       = repository.shortenRemoteBranchName(ref.getName());
                String remoteBranchName = Repository.shortenRefName(ref.getName());

                try
                {
                    git.branchCreate()
                       .setName(branchName)
                       .setUpstreamMode(SetupUpstreamMode.TRACK)
                       .setStartPoint(remoteBranchName)
                       .call();
                }
                catch (RefAlreadyExistsException e)
                {
                }

                System.out.println("Checkout...");
                git.checkout()
                   .setName(branchName)
                   .setForceRefUpdate(true)
                   .setForced(true)
                   .call();
                System.out.println("Pull...");
                git.pull()
                   .call();
                System.out.println("Done");
            }
        }
    }

    @Ignore("Manually enable to test, since it requires access to local repositories.")
    @Test
    public void testJGit2() throws
                            IOException,
                            GitAPIException
    {
        Path repoDir = Paths.get(System.getenv("HOME"), "git/core");

        try (GitHelper gitHelper = new GitHelper(repoDir))
        {
            Git git = gitHelper.openRepository();

            List<Ref> call = git.branchList()
                                .call();
            for (Ref ref : call)
            {
                System.out.println("Branch: " + ref + " " + ref.getName() + " " + ref.getObjectId()
                                                                                     .getName());
            }

            System.out.println("Now including remote branches:");
            call = git.branchList()
                      .setListMode(ListMode.ALL)
                      .call();
            for (Ref ref : call)
            {
                System.out.println("Branch: " + ref + " " + ref.getName() + " " + ref.getObjectId()
                                                                                     .getName());
            }

            Set<String> cleaned = git.clean()
                                     .setDryRun(true)
                                     .setCleanDirectories(true)
                                     .setIgnore(false)
                                     .call();
            for (String file : cleaned)
            {
                System.out.printf("To be cleaned: %s%n", file);
            }
        }
    }

    @Ignore("Manually enable to test, since it requires access to local repositories.")
    @Test
    public void testGitClone() throws
                               IOException,
                               InvalidRemoteException,
                               TransportException,
                               GitAPIException
    {
        if (credDir == null)
        {
            return; // Skip test if no credentials
        }

        UserInfo user = credDir.findFirstAutomationUser(GitHubHelper.API_CREDENTIALS_SITE, RoleType.Automation);

        Path tmpDir = Files.createTempDirectory(null);
        try
        {
            Path dataDir = tmpDir.resolve("data");
            Path db      = tmpDir.resolve(".git");

            try (GitHelper gitHelper = new GitHelper(dataDir, db))
            {
                gitHelper.setCredentials(user);

                Git git = gitHelper.cloneRepository("https://github.com/optio3/core.git");

                // Note: the call() returns an opened repository already which needs to be closed to avoid file handle leaks!
                System.out.println("Having repository getDirectory: " + git.getRepository()
                                                                           .getDirectory());
                System.out.println("Having repository getWorkTree: " + git.getRepository()
                                                                          .getWorkTree());

                FileSystem.deleteDirectory(dataDir);
            }

            try (GitHelper gitHelper = new GitHelper(dataDir, db))
            {
                Git git = gitHelper.openRepository();

                git.reset()
                   .setMode(ResetType.HARD)
                   .call();

                Set<String> cleaned = git.clean()
                                         .setDryRun(true)
                                         .setCleanDirectories(true)
                                         .setIgnore(false)
                                         .call();
                for (String file : cleaned)
                {
                    System.out.printf("To be cleaned: %s%n", file);
                }
            }
        }
        finally
        {
            FileSystem.deleteDirectory(tmpDir);
        }
    }

    @Ignore("Manually enable to test, since it requires access to local repositories.")
    @Test
    public void testListCommits() throws
                                  IOException
    {
        Path repoDir = Paths.get(System.getenv("HOME"), "git/third_party/jersey");

        try (GitHelper gitHelper = new GitHelper(repoDir))
        {
            gitHelper.listCommits(null, -1, 100, (ci) ->
            {
                System.out.printf("%s: %s - %s - %s%n", ci.id, ci.message.split("\n")[0], ci.author.emailAddress, ci.author.toLocalWhen());
            });
        }
    }
}
