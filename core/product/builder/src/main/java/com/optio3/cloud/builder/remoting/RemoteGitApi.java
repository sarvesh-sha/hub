/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.remoting;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.optio3.cloud.builder.model.jobs.input.CommitDetails;
import com.optio3.cloud.client.Optio3RemotableProxy;
import com.optio3.infra.directory.UserInfo;
import com.optio3.util.function.FunctionWithException;

@Optio3RemotableProxy
public interface RemoteGitApi
{
    CompletableFuture<Void> clone(String gitUrl,
                                  Path dbDir,
                                  Path dataDir,
                                  UserInfo credentials,
                                  FunctionWithException<String, CompletableFuture<Void>> progressCallback) throws
                                                                                                           Exception;

    CompletableFuture<List<CommitDetails>> describeBranches(String gitUrl,
                                                            Path dbDir,
                                                            Path dataDir,
                                                            UserInfo credentials) throws
                                                                                  Exception;

    CompletableFuture<CommitDetails> switchToBranch(String gitUrl,
                                                    Path dbDir,
                                                    Path dataDir,
                                                    UserInfo credentials,
                                                    String branch,
                                                    String commit,
                                                    FunctionWithException<String, CompletableFuture<Void>> progressCallback) throws
                                                                                                                             Exception;

    CompletableFuture<Void> enumerateCommits(Path dbDir,
                                             Path dataDir,
                                             String head,
                                             int skip,
                                             int maxCount,
                                             int batchSize,
                                             FunctionWithException<List<CommitDetails>, CompletableFuture<Boolean>> callback) throws
                                                                                                                              Exception;

    CompletableFuture<Void> reset(Path dbDir,
                                  Path dataDir) throws
                                                Exception;

    CompletableFuture<Set<String>> clean(Path dbDir,
                                         Path dataDir,
                                         boolean dryRun) throws
                                                         Exception;
}
