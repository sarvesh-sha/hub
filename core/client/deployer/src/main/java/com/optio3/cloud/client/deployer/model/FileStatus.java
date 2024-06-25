/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.deployer.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.ZonedDateTime;

import com.optio3.util.TimeUtils;

public class FileStatus
{
    public String  name;
    public long    length;
    public boolean isFile;

    public String permissions;
    public String owner;
    public String group;

    public ZonedDateTime creationTime;
    public ZonedDateTime lastAccessTime;
    public ZonedDateTime lastModifiedTime;

    //--//

    public static FileStatus build(Path root,
                                   File f) throws
                                           IOException
    {
        FileStatus fs = new FileStatus();
        fs.name = root.relativize(f.toPath())
                      .toString();
        fs.isFile = f.isFile();

        if (fs.isFile)
        {
            fs.length = f.length();
        }

        PosixFileAttributes attr = Files.readAttributes(f.toPath(), PosixFileAttributes.class);

        fs.creationTime = convert(attr.creationTime());
        fs.lastAccessTime = convert(attr.lastAccessTime());
        fs.lastModifiedTime = convert(attr.lastModifiedTime());

        fs.permissions = PosixFilePermissions.toString(attr.permissions());
        fs.group = attr.group()
                       .getName();
        fs.owner = attr.owner()
                       .getName();

        return fs;
    }

    private static ZonedDateTime convert(FileTime time)
    {
        return TimeUtils.fromInstantToLocalTime(time.toInstant());
    }
}
