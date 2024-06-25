/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.archive;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;

import com.optio3.util.BoxingUtils;
import com.optio3.util.FileSystem;
import com.optio3.util.TimeUtils;
import org.apache.commons.io.FileUtils;
import org.kamranzafar.jtar.TarEntry;
import org.kamranzafar.jtar.TarHeader;
import org.kamranzafar.jtar.TarInputStream;

public class TarArchiveEntry
{
    public enum FileType
    {
        // @formatter:off
        Normal   (TarHeader.LF_NORMAL ),
        Link     (TarHeader.LF_LINK   ),
        SymLink  (TarHeader.LF_SYMLINK),
        Character(TarHeader.LF_CHR    ),
        Block    (TarHeader.LF_BLK    ),
        Directory(TarHeader.LF_DIR    ),
        Fifo     (TarHeader.LF_FIFO   ),
        Contig   (TarHeader.LF_CONTIG );
        // @formatter:on

        private final byte m_value;

        FileType(byte c)
        {
            m_value = c;
        }

        public static FileType parse(byte c)
        {
            switch (c)
            {
                // @formatter:off
                case TarHeader.LF_NORMAL : return Normal;
                case TarHeader.LF_LINK   : return Link;
                case TarHeader.LF_SYMLINK: return SymLink;
                case TarHeader.LF_CHR    : return Character;
                case TarHeader.LF_BLK    : return Block;
                case TarHeader.LF_DIR    : return Directory;
                case TarHeader.LF_FIFO   : return Fifo;
                case TarHeader.LF_CONTIG : return Contig;
                default                  : return null;
                // @formatter:on
            }
        }

        public byte getEncoding()
        {
            return m_value;
        }
    }

    private final TarInputStream m_tarInput;
    private       long           m_headerOffsetStart;
    private       long           m_headerOffsetEnd;
    private       InputStream    m_stream;

    public String name;
    public int    permissions;
    public int    userId;
    public int    groupId;
    public long   size;
    public long   modTime;
    public int    checkSum;
    public byte   linkFlag;
    public String linkName;
    public String magic; // ustar indicator and version
    public String userName;
    public String groupName;
    public int    devMajor;
    public int    devMinor;

    TarArchiveEntry(TarInputStream tarInput,
                    TarEntry entry,
                    long offsetStart,
                    long offsetEnd)
    {
        m_tarInput = tarInput;
        m_headerOffsetStart = offsetStart;
        m_headerOffsetEnd = offsetEnd;

        TarHeader header = entry.getHeader();

        String name = entry.getName();
        if (name.startsWith("./"))
        {
            name = name.substring(2);
        }

        this.name = name;
        this.permissions = header.mode;
        this.userId = header.userId;
        this.groupId = header.groupId;
        this.size = header.size;
        this.modTime = header.modTime;
        this.checkSum = header.checkSum;
        this.linkFlag = header.linkFlag;
        this.linkName = fromStringBuffer(header.linkName);
        this.magic = fromStringBuffer(header.magic);
        this.userName = fromStringBuffer(header.userName);
        this.groupName = fromStringBuffer(header.groupName);
        this.devMajor = header.devMajor;
        this.devMinor = header.devMinor;
    }

    private TarArchiveEntry(String name,
                            long size,
                            int permissions)
    {
        m_tarInput = null;
        m_headerOffsetEnd = -1;
        m_headerOffsetStart = -1;

        this.name = name;
        this.size = size;
        this.permissions = permissions;

        this.userName = "root";
        this.groupName = "root";
    }

    private TarArchiveEntry(FileType type,
                            String name,
                            long size,
                            int permissions,
                            ZonedDateTime modTime)
    {
        this(name, size, permissions);

        this.linkFlag = type.getEncoding();

        this.setLastModifiedTime(modTime != null ? modTime : TimeUtils.now());
    }

    public static TarArchiveEntry newRaw(String name,
                                         long size,
                                         int permissions)
    {
        return new TarArchiveEntry(name, size, permissions);
    }

    public static TarArchiveEntry newFile(String name,
                                          long size,
                                          int permissions,
                                          ZonedDateTime modTime)
    {
        return new TarArchiveEntry(FileType.Normal, name, size, permissions, modTime);
    }

    public static TarArchiveEntry newDirectory(String name,
                                               int permissions,
                                               ZonedDateTime modTime)
    {
        return new TarArchiveEntry(FileType.Directory, name, 0, permissions, modTime);
    }

    //--//

    public boolean isDirectory()
    {
        return FileType.parse(linkFlag) == FileType.Directory;
    }

    public boolean isLink()
    {
        switch (FileType.parse(linkFlag))
        {
            case Link:
            case SymLink:
                return true;

            default:
                return false;
        }
    }

    //--//

    public long getHeaderOffset()
    {
        return m_headerOffsetStart;
    }

    public long getHeaderLength()
    {
        return m_headerOffsetEnd - m_headerOffsetStart;
    }

    public ZonedDateTime getLastModifiedTime()
    {
        return TimeUtils.fromSecondsToLocalTime(modTime);
    }

    public void setLastModifiedTime(ZonedDateTime newTime)
    {
        modTime = newTime.toEpochSecond();
    }

    //--//

    public InputStream getStream()
    {
        if (m_stream == null && m_tarInput != null)
        {
            m_stream = new InputStream()
            {
                private long m_available = size;

                @Override
                public int read() throws
                                  IOException
                {
                    int res = m_available > 0 ? m_tarInput.read() : -1;
                    if (res >= 0)
                    {
                        m_available--;
                    }

                    return res;
                }

                @Override
                public int read(byte[] b,
                                int off,
                                int len) throws
                                         IOException
                {
                    int available = (int) Math.min(m_available, len);

                    int read = m_tarInput.read(b, off, available);

                    if (read > 0)
                    {
                        m_available -= read;
                    }

                    return read;
                }
            };
        }

        return m_stream;
    }

    //--//

    TarEntry convertToExportFormat()
    {
        TarHeader header = TarHeader.createHeader(name, size, modTime, isDirectory(), permissions);

        header.userId = userId;
        header.groupId = groupId;

        header.linkFlag = linkFlag;
        header.linkName = toStringBuffer(linkName);
        header.magic = toStringBuffer(magic);

        header.userName = toStringBuffer(userName);
        header.groupName = toStringBuffer(groupName);
        header.devMajor = devMajor;
        header.devMinor = devMinor;

        return new TarEntry(header);
    }

    //--//

    public FileSystem.TmpFileHolder saveToDiskAsTempFile() throws
                                                           IOException
    {
        final FileSystem.TmpFileHolder tmpFile     = FileSystem.createTempFile();
        boolean                        shouldClose = true;

        try
        {
            saveToDisk(tmpFile.get());
            shouldClose = false;
            return tmpFile;
        }
        finally
        {
            if (shouldClose)
            {
                tmpFile.close();
            }
        }
    }

    public void saveToDisk(File destination) throws
                                             IOException
    {
        FileUtils.copyToFile(getStream(), destination);
    }

    //--//

    private static String fromStringBuffer(StringBuffer buf)
    {
        return buf != null && buf.length() > 0 ? buf.toString() : null;
    }

    private static StringBuffer toStringBuffer(String text)
    {
        return new StringBuffer(BoxingUtils.get(text, ""));
    }
}
