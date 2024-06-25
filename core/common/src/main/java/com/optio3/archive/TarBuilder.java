/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.archive;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import com.optio3.util.TimeUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.kamranzafar.jtar.PermissionUtils;
import org.kamranzafar.jtar.TarOutputStream;

public class TarBuilder implements AutoCloseable
{
    private final TarOutputStream  m_tar;
    private final GZIPOutputStream m_zip;

    private ZonedDateTime m_overrideTime;

    public TarBuilder(OutputStream stream,
                      boolean compress) throws
                                        IOException
    {
        if (compress)
        {
            m_zip = new GZIPOutputStream(stream);
            stream = m_zip;
        }
        else
        {
            m_zip = null;
        }

        m_tar = new TarOutputStream(stream);
    }

    @Override
    public void close() throws
                        IOException
    {
        m_tar.close();

        if (m_zip != null)
        {
            m_zip.close();
        }
    }

    public void setOverrideTime(ZonedDateTime time)
    {
        m_overrideTime = time;
    }

    //--//

    public static void packDirectory(OutputStream stream,
                                     File dir,
                                     boolean compress,
                                     ZonedDateTime overrideTime) throws
                                                                 IOException
    {
        try (TarBuilder builder = new TarBuilder(stream, compress))
        {
            if (overrideTime != null)
            {
                builder.setOverrideTime(overrideTime);
            }

            builder.addAllFiles(null, dir);
        }
    }

    public static byte[] packDirectory(File dir,
                                       boolean compress,
                                       ZonedDateTime overrideTime) throws
                                                                   IOException
    {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();

        packDirectory(buf, dir, compress, overrideTime);

        return buf.toByteArray();
    }

    //--//

    public void add(TarArchiveEntry entry) throws
                                           IOException
    {
        if (m_overrideTime != null)
        {
            entry.setLastModifiedTime(m_overrideTime);
        }

        m_tar.putNextEntry(entry.convertToExportFormat());
    }

    public void add(String tarPath,
                    File f) throws
                            IOException
    {
        tarPath = tarPath == null ? "/" : tarPath + "/";
        tarPath += f.getName();

        if (f.isFile())
        {
            int           permissions = PermissionUtils.permissions(f);
            ZonedDateTime modTime     = TimeUtils.fromSecondsToLocalTime(f.lastModified());

            add(TarArchiveEntry.newFile(tarPath, f.length(), permissions, modTime));
            FileUtils.copyFile(f, m_tar);
        }
        else if (f.isDirectory())
        {
            addAllFiles(tarPath, f);
        }
    }

    public String addAsDir(String tarPath,
                           String dirName,
                           int permissions) throws
                                            IOException
    {
        tarPath = StringUtils.isEmpty(tarPath) ? "/" : tarPath + "/";
        tarPath += dirName;

        add(TarArchiveEntry.newDirectory(tarPath, permissions, null));

        return tarPath;
    }

    public String addAsString(String tarPath,
                              String fileName,
                              String contents) throws
                                               IOException
    {
        return addAsString(tarPath, fileName, contents, 0444);
    }

    public String addAsString(String tarPath,
                              String fileName,
                              String contents,
                              int permissions) throws
                                               IOException
    {
        return addAsBytes(tarPath, fileName, contents.getBytes(), permissions);
    }

    public String addAsBytes(String tarPath,
                             String fileName,
                             byte[] data,
                             int permissions) throws
                                              IOException
    {
        tarPath = tarPath == null ? "/" : tarPath + "/";
        tarPath += fileName;

        TarArchiveEntry entry = TarArchiveEntry.newFile(tarPath, data.length, permissions, null);
        add(entry);
        m_tar.write(data);

        return tarPath;
    }

    public String addAsStream(String tarPath,
                              String fileName,
                              InputStream data,
                              int length,
                              int permissions) throws
                                               IOException
    {
        tarPath = tarPath == null ? "/" : tarPath + "/";
        tarPath += fileName;

        TarArchiveEntry entry = TarArchiveEntry.newFile(tarPath, length, permissions, null);
        addAsStream(entry, data);

        return tarPath;
    }

    public void addAsStream(TarArchiveEntry entry,
                            InputStream data) throws
                                              IOException
    {
        add(entry);

        IOUtils.copyLarge(data, m_tar);
    }

    public void copyFromZip(File file) throws
                                       Exception
    {
        //
        // TAR wants to know the size of each entry.
        // ZIP sometimes doesn't put the size in the header.
        // We have to walk the zip file to compute the file sizes.
        //
        Map<String, Long> sizes = ZipWalker.computeSizes(file);

        ZipWalker.walk(file, (subEntry) ->
        {
            String name = subEntry.getName();
            Long   size = sizes.get(name);
            if (size != null) // If we have a size, it's a file.
            {
                Path path    = Paths.get(name);
                Path dirPart = path.getParent();

                String dirText;
                String fileText;

                if (dirPart != null)
                {
                    dirText = dirPart.toString();
                    fileText = dirPart.relativize(path)
                                      .toString();
                }
                else
                {
                    dirText = null;
                    fileText = path.toString();
                }

                addAsStream(dirText, fileText, subEntry.getStream(), (int) (long) size, 0664);
            }
            return true;
        });
    }

    //--//

    private void addAllFiles(String tarPath,
                             File root) throws
                                        IOException
    {
        File[] files = root.listFiles();

        //
        // First files, then directories, it makes for a better-looking tar listing.
        //
        for (File f : files)
        {
            if (f.isFile())
            {
                add(tarPath, f);
            }
        }

        for (File f : files)
        {
            if (f.isDirectory())
            {
                add(tarPath, f);
            }
        }
    }
}
