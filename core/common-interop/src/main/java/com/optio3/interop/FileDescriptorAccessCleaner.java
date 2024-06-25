/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

package com.optio3.interop;

import java.nio.file.ClosedFileSystemException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.optio3.concurrency.Executors;
import com.optio3.logging.Logger;
import com.optio3.util.ResourceCleaner;
import com.optio3.util.TimeUtils;
import com.sun.jna.Structure;

public class FileDescriptorAccessCleaner extends ResourceCleaner
{
    public static final Logger LoggerInstance = new Logger(FileDescriptorAccessCleaner.class);

    //--//

    static class CallMonitor implements AutoCloseable
    {
        final FileDescriptorAccessCleaner target;
        final Thread                      thread;

        boolean stale;
        boolean staleWarning;

        CallMonitor(FileDescriptorAccessCleaner target)
        {
            this.target = target;
            thread      = Thread.currentThread();

            s_callMonitors.link(this);
        }

        @Override
        public void close()
        {
            ScheduledFuture<?> purger = s_callMonitors.unlink(this);
            if (purger != null)
            {
                purger.cancel(false);
            }
        }
    }

    static class CallMonitorList
    {
        final LinkedList<CallMonitor> chain = new LinkedList<>();
        ScheduledFuture<?> purger;

        synchronized void link(CallMonitor pm)
        {
            chain.add(pm);

            ensureScheduled();
        }

        synchronized ScheduledFuture<?> unlink(CallMonitor pm)
        {
            chain.remove(pm);

            if (chain.isEmpty())
            {
                var purgerToCancel = purger;
                purger = null;

                return purgerToCancel;
            }

            return null;
        }

        void processStaleCalls()
        {
            purger = null;

            CallMonitor ptrStale = popNextStale();
            if (ptrStale != null)
            {
                try
                {
                    ptrStale.thread.interrupt();
                    ptrStale.target.clean();
                }
                catch (Throwable t)
                {
                    // Ignore failures.
                    LoggerInstance.warn("Failed to close '%s', due to %s", ptrStale.target.m_handleContext, t);
                }
            }

            ensureScheduledIfNeeded();
        }

        private synchronized CallMonitor popNextStale()
        {
            for (CallMonitor pm : chain)
            {
                if (pm.stale)
                {
                    if (pm.staleWarning)
                    {
                        LoggerInstance.debug("Detected stale call for '%s'", pm.target.m_handleContext);
                    }
                    else
                    {
                        pm.staleWarning = true;

                        LoggerInstance.warn("Detected stale call for '%s'", pm.target.m_handleContext);
                    }

                    return pm;
                }

                pm.stale = true;
            }

            return null;
        }

        private synchronized void ensureScheduledIfNeeded()
        {
            if (!chain.isEmpty())
            {
                ensureScheduled();
            }
        }

        private void ensureScheduled()
        {
            if (purger == null)
            {
                purger = Executors.scheduleOnDefaultPool(this::processStaleCalls, 15, TimeUnit.SECONDS);
            }
        }
    }

    //--//

    private static final CallMonitorList s_callMonitors = new CallMonitorList();

    private       int                         m_handle;
    private       String                      m_handleContext;
    private final FileDescriptorAccess.pollfd m_pollfd = new FileDescriptorAccess.pollfd();

    public FileDescriptorAccessCleaner(Object holder)
    {
        super(holder);
    }

    @Override
    protected void closeUnderCleaner()
    {
        if (m_handle > 0)
        {
            FileDescriptorAccess.close(m_handle);
            m_handle = 0;
        }
    }

    public void setHandle(int handle,
                          String context)
    {
        m_handle        = handle;
        m_handleContext = context;
    }

    public int getHandle()
    {
        return ensureHandleOpen();
    }

    public <T extends Structure> int ioctl(int svc,
                                           T data)
    {
        try (CallMonitor monitor = new CallMonitor(this))
        {
            return FileDescriptorAccess.ioctl(ensureHandleOpen(), svc, data);
        }
        catch (Throwable t)
        {
            return -1;
        }
    }

    public int ioctl(int svc,
                     int value)
    {
        try (CallMonitor monitor = new CallMonitor(this))
        {
            return FileDescriptorAccess.ioctl(ensureHandleOpen(), svc, value);
        }
        catch (Throwable t)
        {
            return -1;
        }
    }

    public int poll(int timeout,
                    ChronoUnit unit)
    {
        int msec;

        if (unit != null)
        {
            Duration duration = TimeUtils.computeSafeDuration(timeout, unit);

            msec = (int) duration.toMillis();
        }
        else
        {
            msec = 100;
        }

        msec = Math.min(msec, 1000);
        msec = Math.max(msec, 50);

        //
        // When running under Docker, read() on a socket might hang, not even returning when the socket is closed.
        // So we use poll(), returning 0 for "no data available".
        //
        m_pollfd.fd     = ensureHandleOpen();
        m_pollfd.events = FileDescriptorAccess.pollfd.POLLIN | FileDescriptorAccess.pollfd.POLLHUP | FileDescriptorAccess.pollfd.POLLRDNORM;

        try (CallMonitor monitor = new CallMonitor(this))
        {
            return FileDescriptorAccess.poll(m_pollfd, 1, msec);
        }
        catch (Throwable t)
        {
            return -1;
        }
    }

    public int readBuffer(byte[] buffer,
                          int length)
    {
        try (CallMonitor monitor = new CallMonitor(this))
        {
            return FileDescriptorAccess.read(ensureHandleOpen(), buffer, length);
        }
        catch (Throwable t)
        {
            return -1;
        }
    }

    public <T extends Structure> int readStruct(T struct)
    {
        try (CallMonitor monitor = new CallMonitor(this))
        {
            int res = FileDescriptorAccess.read(ensureHandleOpen(), struct.getPointer(), struct.size());

            struct.read();

            return res;
        }
        catch (Throwable t)
        {
            return -1;
        }
    }

    public void writeBuffer(byte[] buffer,
                            int length)
    {
        try (CallMonitor monitor = new CallMonitor(this))
        {
            int res = FileDescriptorAccess.write(ensureHandleOpen(), buffer, length);
            FileDescriptorAccess.checkResult(res);
        }
    }

    public <T extends Structure> void writeStruct(T struct)
    {
        try (CallMonitor monitor = new CallMonitor(this))
        {
            struct.write();

            int res = FileDescriptorAccess.write(ensureHandleOpen(), struct.getPointer(), struct.size());
            FileDescriptorAccess.checkResult(res);
        }
    }

    //--//

    private int ensureHandleOpen()
    {
        int handle = m_handle;
        if (handle <= 0)
        {
            throw new ClosedFileSystemException();
        }

        return handle;
    }
}
