/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.concurrency;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;

public class DelayedAction
{
    private final Runnable           m_command;
    private       ScheduledFuture<?> m_future;

    public DelayedAction(Runnable command)
    {
        m_command = command;
    }

    public void schedule(MonotonousTime when)
    {
        Duration delay = TimeUtils.remainingTime(when);
        schedule(delay != null ? delay.toMillis() : 0, TimeUnit.MILLISECONDS);
    }

    public void schedule(long delay,
                         TimeUnit unit)
    {
        schedule(Executors.getDefaultScheduledExecutor(), delay, unit);
    }

    public synchronized void schedule(ScheduledExecutorService executor,
                                      long delay,
                                      TimeUnit unit)
    {
        cancelNoLock();

        m_future = executor.schedule(() ->
                                     {
                                         cancel();
                                         m_command.run();
                                     }, delay, unit);
    }

    public boolean isScheduled()
    {
        ScheduledFuture<?> future = m_future;
        return future != null && !future.isDone();
    }

    public synchronized void cancel()
    {
        cancelNoLock();
    }

    private void cancelNoLock()
    {
        if (m_future != null)
        {
            m_future.cancel(false);
            m_future = null;
        }
    }
}