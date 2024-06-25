/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.remoting;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import com.optio3.cloud.client.Optio3RemotableProxy;

@Optio3RemotableProxy
public interface RemoteFileSystemApi
{
    CompletableFuture<Void> createDirectory(Path path);

    CompletableFuture<Void> deleteDirectory(Path path);

    CompletableFuture<Boolean> deleteFile(Path path);

    CompletableFuture<Void> writeFile(Path path,
                                      byte[] data) throws
                                                   Exception;

    CompletableFuture<Void> appendFile(Path path,
                                       byte[] data) throws
                                                    Exception;

    CompletableFuture<Long> fileSize(Path path);

    CompletableFuture<byte[]> readFile(Path path,
                                       int offset,
                                       int count) throws
                                                  Exception;
}
