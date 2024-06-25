/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.interop;

import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.util.Exceptions;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Platform;
import com.sun.jna.Structure;

public final class UnixSocketAdapter
{
    private static final int PF_UNIX     = 1;
    private static final int SOCK_STREAM = 1;
    private static final int AF_UNIX     = 1;

    public static abstract class sockaddr_un extends Structure
    {
        public          byte[] sun_path = new byte[108];
        protected final int    m_path_len;

        protected sockaddr_un(String path)
        {
            byte[] buf = Native.toByteArray(path);

            m_path_len = buf.length;

            System.arraycopy(buf, 0, sun_path, 0, m_path_len);
        }

        public abstract int length();
    }

    // Linux specific
    public static class sockaddr_un__linux extends sockaddr_un
    {
        public short sun_family;

        public sockaddr_un__linux(String path)
        {
            super(path);

            sun_family = AF_UNIX;
        }

        @Override
        protected List<String> getFieldOrder()
        {
            return Lists.newArrayList("sun_family", "sun_path");
        }

        @Override
        public int length()
        {
            return 2 + m_path_len;
        }
    }

    // Mac OS X and BSD specific
    public static class sockaddr_un__bsd extends sockaddr_un
    {
        public byte sun_len;
        public byte sun_family;

        public sockaddr_un__bsd(String path)
        {
            super(path);

            sun_family = AF_UNIX;
        }

        @Override
        protected List<String> getFieldOrder()
        {
            return Lists.newArrayList("sun_len", "sun_family", "sun_path");
        }

        @Override
        public int length()
        {
            return 2 + m_path_len;
        }
    }

    //--//

    static
    {
        Native.register(UnixSocketAdapter.class, NativeLibrary.getInstance("c"));
    }

    private static native int socket(int domain,
                                     int type,
                                     int protocol);

    private static native int connect(int s,
                                      sockaddr_un name,
                                      int namelen);

    //--//

    private final FileDescriptorAccessCleaner m_state = new FileDescriptorAccessCleaner(this);

    public UnixSocketAdapter(String path)
    {
        sockaddr_un addr;

        if (Platform.isMac())
        {
            addr = new sockaddr_un__bsd(path);
        }
        else
        {
            addr = new sockaddr_un__linux(path);
        }

        int res = socket(PF_UNIX, SOCK_STREAM, 0);
        if (res <= 0)
        {
            throw Exceptions.newRuntimeException("Socket failed: %s", res);
        }

        m_state.setHandle(res, path);

        res = connect(m_state.getHandle(), addr, addr.length());
        if (res < 0)
        {
            throw Exceptions.newRuntimeException("Connect failed: %s", res);
        }
    }

    public void close()
    {
        m_state.clean();
    }

    public int read(byte[] buffer,
                    int timeout)
    {
        int res = m_state.poll(timeout, ChronoUnit.MILLIS);
        if (res <= 0)
        {
            return res;
        }

        return m_state.readBuffer(buffer, buffer.length);
    }

    public void write(byte[] buffer,
                      int len)
    {
        m_state.writeBuffer(buffer, len);
    }

    public void write(String text)
    {
        byte[] buf = StandardCharsets.UTF_8.encode(text)
                                           .array();
        write(buf, buf.length);
    }
}
