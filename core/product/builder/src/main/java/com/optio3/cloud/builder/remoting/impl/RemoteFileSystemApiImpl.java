/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.remoting.impl;

import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import com.optio3.cloud.annotation.Optio3RemotableEndpoint;
import com.optio3.cloud.builder.remoting.RemoteFileSystemApi;
import com.optio3.util.FileSystem;

@Optio3RemotableEndpoint(itf = RemoteFileSystemApi.class)
public final class RemoteFileSystemApiImpl implements RemoteFileSystemApi
{
    @Override
    public CompletableFuture<Void> createDirectory(Path dir)
    {
        File file = dir.toFile();
        if (!file.isDirectory())
        {
            file.mkdirs();
        }

        return wrapAsync(null);
    }

    @Override
    public CompletableFuture<Void> deleteDirectory(Path dir)
    {
        if (dir.toFile()
               .isDirectory())
        {
            FileSystem.deleteDirectory(dir);
        }

        return wrapAsync(null);
    }

    @Override
    public CompletableFuture<Boolean> deleteFile(Path path)
    {
        return wrapAsync(path.toFile()
                             .delete());
    }

    @Override
    public CompletableFuture<Void> writeFile(Path path,
                                             byte[] data) throws
                                                          Exception
    {
        path.getParent()
            .toFile()
            .mkdirs();

        try (FileOutputStream output = new FileOutputStream(path.toFile()))
        {
            output.write(data, 0, data.length);
        }

        return wrapAsync(null);
    }

    @Override
    public CompletableFuture<Void> appendFile(Path path,
                                              byte[] data) throws
                                                           Exception
    {
        path.getParent()
            .toFile()
            .mkdirs();

        try (FileOutputStream output = new FileOutputStream(path.toFile(), true))
        {
            output.write(data, 0, data.length);
        }

        return wrapAsync(null);
    }

    @Override
    public CompletableFuture<Long> fileSize(Path path)
    {
        File file = path.toFile();

        if (file.isFile())
        {
            return wrapAsync(file.length());
        }

        return wrapAsync(null);
    }

    @Override
    public CompletableFuture<byte[]> readFile(Path path,
                                              int offset,
                                              int count) throws
                                                         Exception
    {
        File file = path.toFile();
        if (!file.isFile())
        {
            return wrapAsync(null);
        }

        if (count < 0)
        {
            count = (int) file.length();
        }

        byte[] res = new byte[count];
        int    read;

        try (FileInputStream input = new FileInputStream(file))
        {
            read = input.read(res, offset, count);
        }

        if (read < 0)
        {
            return wrapAsync(null);
        }

        if (read < count)
        {
            res = Arrays.copyOf(res, read);
        }

        return wrapAsync(res);
    }
}
