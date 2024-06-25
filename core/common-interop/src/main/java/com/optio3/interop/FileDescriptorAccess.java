/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

package com.optio3.interop;

import java.util.List;

import com.google.common.collect.Lists;
import com.sun.jna.LastErrorException;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

public final class FileDescriptorAccess
{
    public static class pollfd extends Structure
    {
        /*
         * Requestable events. If poll(2) finds any of these set, they are
         * copied to revents on return.
         */
        static final int POLLIN     = 0x0001; /* any readable data available */
        static final int POLLPRI    = 0x0002; /* OOB/Urgent readable data */
        static final int POLLOUT    = 0x0004; /* file descriptor is writeable */
        static final int POLLRDNORM = 0x0040; /* non-OOB/URG data available */
        static final int POLLWRNORM = POLLOUT; /* no write type differentiation */
        static final int POLLRDBAND = 0x0080; /* OOB/Urgent readable data */
        static final int POLLWRBAND = 0x0100; /* OOB/Urgent data can be written */

        /*
         * FreeBSD extensions: polling on a regular file might return one
         * of these events (currently only supported on local filesystems).
         */
        static final int POLLEXTEND = 0x0200; /* file may have been extended */
        static final int POLLATTRIB = 0x0400; /* file attributes may have changed */
        static final int POLLNLINK  = 0x0800; /* (un)link/rename may have happened */
        static final int POLLWRITE  = 0x1000; /* file's contents may have changed */

        /*
         * These events are set if they occur regardless of whether they were
         * requested.
         */
        static final int POLLERR  = 0x0008; /* some poll error occurred */
        static final int POLLHUP  = 0x0010; /* file descriptor was "hung up" */
        static final int POLLNVAL = 0x0020; /* requested events "invalid" */

        static final int POLLSTANDARD = (POLLIN | POLLPRI | POLLOUT | POLLRDNORM | POLLRDBAND | POLLWRBAND | POLLERR | POLLHUP | POLLNVAL);

        public int  fd;
        public char events;
        public char revents;

        @Override
        protected List<String> getFieldOrder()
        {
            return Lists.newArrayList("fd", "events", "revents");
        }
    }

    //--//

    static
    {
        Native.register(FileDescriptorAccess.class, NativeLibrary.getInstance("c"));
    }

    public static native int open(String device,
                                  int flags);

    public static native int close(int s);

    public static native int flock(int fd,
                                   int op);

    static native int poll(pollfd fds,
                           int nfds,
                           int timeout);

    static native int read(int s,
                           byte[] data,
                           long len);

    static native int write(int s,
                            byte[] data,
                            long len);

    static native int read(int s,
                           Pointer data,
                           long len);

    static native int write(int s,
                            Pointer data,
                            long len);

    static native <T extends Structure> int ioctl(int fd,
                                                  int svc,
                                                  T data);

    static native <T extends Structure> int ioctl(int fd,
                                                  int svc,
                                                  int value);

    public static void checkResult(int res)
    {
        if (res < 0)
        {
            int errno = Native.getLastError();
            throw new LastErrorException(errno);
        }
    }
}
