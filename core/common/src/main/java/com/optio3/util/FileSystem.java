/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.google.common.collect.Maps;
import com.optio3.util.function.ConsumerWithException;
import org.apache.commons.io.IOUtils;

public class FileSystem
{
    private static File s_baseTempDirectory;
    private static Path s_baseTempDirectoryPath;

    public static class TmpFileHolder implements AutoCloseable
    {
        private File m_file;

        TmpFileHolder(File file)
        {
            m_file = file;
        }

        public void disableAutoDelete()
        {
            m_file = null;
        }

        public File get()
        {
            return m_file;
        }

        public String getAbsolutePath()
        {
            return m_file.getAbsolutePath();
        }

        public boolean delete()
        {
            return m_file.delete();
        }

        public byte[] read() throws
                             IOException
        {
            return com.google.common.io.Files.toByteArray(m_file);
        }

        public void write(byte[] buf) throws
                                      IOException
        {
            com.google.common.io.Files.write(buf, m_file);
        }

        @Override
        public void close()
        {
            if (m_file != null)
            {
                m_file.delete();
                m_file = null;
            }
        }
    }

    public static class TmpDirHolder implements AutoCloseable
    {
        private final Path m_tmpDir;

        TmpDirHolder(String prefix) throws
                                    IOException
        {
            if (s_baseTempDirectoryPath != null)
            {
                m_tmpDir = Files.createTempDirectory(s_baseTempDirectoryPath, prefix);
            }
            else
            {
                m_tmpDir = Files.createTempDirectory(prefix);
            }
        }

        public Path get()
        {
            return m_tmpDir;
        }

        public String getAbsolutePath()
        {
            return m_tmpDir.toFile()
                           .getAbsolutePath();
        }

        public boolean delete()
        {
            return FileSystem.deleteDirectory(m_tmpDir)
                             .isEmpty();
        }

        @Override
        public void close()
        {
            delete();
        }
    }

    //--//

    public static class Blob implements AutoCloseable
    {
        private final String        m_prefix;
        private       TmpFileHolder m_blob;
        private       long          m_size;

        public Blob(String prefix)
        {
            m_prefix = prefix;
        }

        @Override
        public void close()
        {
            if (m_blob != null)
            {
                try
                {
                    m_blob.close();
                }
                catch (Throwable e)
                {
                    // Ignore delete failures on temporary files.
                }

                m_blob = null;
            }
        }

        public long getSize()
        {
            return m_size;
        }

        public void setSize(long size)
        {
            close();

            m_size = size;
        }

        public boolean isActive()
        {
            return m_blob != null;
        }

        public void setContents(InputStream input,
                                long size) throws
                                           IOException
        {
            try (FileOutputStream output = openWriteStream())
            {
                long bytesCopied = IOUtils.copyLarge(input, output, 0, size);
                if (bytesCopied != size)
                {
                    throw Exceptions.newRuntimeException("Failed to import contents: expected = %,d , actual = %,d", size, bytesCopied);
                }

                m_size = bytesCopied;
            }
        }

        public void compressContents(Blob uncompressed,
                                     long sizeUncompressed) throws
                                                            IOException
        {
            try (FileInputStream input = uncompressed.openReadStream())
            {
                compressContents(input, sizeUncompressed);
            }
        }

        public void compressContents(InputStream input,
                                     long sizeUncompressed) throws
                                                            IOException
        {
            try (FileOutputStream output = openWriteStream())
            {
                try (GZIPOutputStream outputCompressed = new GZIPOutputStream(output))
                {
                    long bytesCopied = IOUtils.copyLarge(input, outputCompressed, 0, sizeUncompressed);
                    if (bytesCopied != sizeUncompressed)
                    {
                        throw Exceptions.newRuntimeException("Failed to compress contents: expected = %,d , actual = %,d", sizeUncompressed, bytesCopied);
                    }
                }
            }

            m_size = ensureBlob().length();
        }

        public void uncompressContents(Blob compressed) throws
                                                        IOException
        {
            try (FileInputStream input = compressed.openReadStream())
            {
                try (GZIPInputStream inputUncompressed = new GZIPInputStream(input))
                {
                    setContents(inputUncompressed, m_size);
                }
            }
        }

        public RandomAccessFile openFileForRead() throws
                                                  IOException
        {
            return new RandomAccessFile(ensureBlob().getAbsolutePath(), "r");
        }

        public RandomAccessFile openFileForWrite() throws
                                                   IOException
        {
            RandomAccessFile file = new RandomAccessFile(ensureNewBlob().getAbsolutePath(), "rw");
            file.setLength(m_size);
            return file;
        }

        public FileInputStream openReadStream() throws
                                                IOException
        {
            return new FileInputStream(ensureBlob());
        }

        public FileOutputStream openWriteStream() throws
                                                  IOException
        {
            return new FileOutputStream(ensureNewBlob());
        }

        private File ensureNewBlob() throws
                                     IOException
        {
            close();

            return ensureBlob();
        }

        private File ensureBlob() throws
                                  IOException
        {
            if (m_blob == null)
            {
                m_blob = createTempFile(m_prefix, ".tmp.bin");
            }

            return m_blob.get();
        }
    }

    //--//

    public static void setTempDirectory(File directory) throws
                                                        IOException
    {
        s_baseTempDirectory = directory;
        s_baseTempDirectoryPath = directory.toPath();

        Files.createDirectories(s_baseTempDirectoryPath);
    }

    //--//

    public static TmpFileHolder createTempFile() throws
                                                 IOException
    {
        return createTempFile("tmp-", ".tmp");
    }

    public static TmpFileHolder createTempFile(String prefix,
                                               String suffix) throws
                                                              IOException
    {
        return autoDelete(File.createTempFile(prefix, suffix, s_baseTempDirectory));
    }

    public static TmpFileHolder autoDelete(String path)
    {
        return new TmpFileHolder(new File(path));
    }

    public static TmpFileHolder autoDelete(File file)
    {
        return new TmpFileHolder(file);
    }

    //--//

    public static TmpDirHolder createTempDirectory(String prefix) throws
                                                                  IOException
    {
        return new TmpDirHolder(prefix);
    }

    public static Path resolveTempDirectory(String folder)
    {
        Path root = s_baseTempDirectoryPath;
        if (root == null)
        {
            String tmpDir = System.getProperty("java.io.tmpdir");
            if (tmpDir == null)
            {
                tmpDir = "/tmp";
            }

            root = Path.of(tmpDir);
        }

        return root.resolve(folder);
    }

    public static void createDirectory(Path dir) throws
                                                 IOException
    {
        if (!Files.isDirectory(dir))
        {
            Files.createDirectories(dir);
        }
    }

    public static Map<Path, IOException> deleteDirectory(Path dir)
    {
        Map<Path, IOException> errors = Maps.newHashMap();

        try
        {
            Files.walkFileTree(dir, new FileVisitor<Path>()
            {
                @Override
                public FileVisitResult preVisitDirectory(Path dir,
                                                         BasicFileAttributes attrs)
                {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file,
                                                 BasicFileAttributes attrs)
                {
                    try
                    {
                        Files.delete(file);
                    }
                    catch (IOException e)
                    {
                        errors.put(file, e);
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file,
                                                       IOException exc)
                {
                    errors.put(file, exc);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir,
                                                          IOException exc)
                {
                    try
                    {
                        Files.delete(dir);
                    }
                    catch (IOException e)
                    {
                        errors.put(dir, e);
                    }

                    return FileVisitResult.CONTINUE;
                }
            });
        }
        catch (IOException e)
        {
            // We log all the exceptions in 'errors'.
        }

        return errors;
    }

    public static Map<Path, Exception> walkDirectory(Path dir,
                                                     Function<Path, Boolean> dirFilter,
                                                     Function<Path, Boolean> fileFilter,
                                                     ConsumerWithException<Path> visit) throws
                                                                                        IOException
    {
        Map<Path, Exception> errors = Maps.newHashMap();

        Files.walkFileTree(dir, new FileVisitor<Path>()
        {
            @Override
            public FileVisitResult preVisitDirectory(Path dir,
                                                     BasicFileAttributes attrs)
            {
                return dirFilter.apply(dir) ? FileVisitResult.CONTINUE : FileVisitResult.SKIP_SUBTREE;
            }

            @Override
            public FileVisitResult visitFile(Path file,
                                             BasicFileAttributes attrs)
            {
                if (fileFilter.apply(file))
                {
                    try
                    {
                        visit.accept(file);
                    }
                    catch (Exception e)
                    {
                        errors.put(file, e);
                    }
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file,
                                                   IOException exc)
            {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir,
                                                      IOException exc)
            {
                return FileVisitResult.CONTINUE;
            }
        });

        return errors;
    }
}
