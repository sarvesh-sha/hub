/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.common;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.optio3.concurrency.Executors;
import com.optio3.logging.ILogger;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;

public abstract class ServiceWorkerWithWatchdog extends ServiceWorker
{
    private final Duration           m_maxInactivity;
    private       MonotonousTime     m_expiration;
    private       ScheduledFuture<?> m_watchdog;

    protected ServiceWorkerWithWatchdog(ILogger logger,
                                        String name,
                                        int failureDelayInSeconds,
                                        int stopMaxWait,
                                        int maxInactivityInSeconds)
    {
        super(logger, name, failureDelayInSeconds, stopMaxWait);

        m_maxInactivity = Duration.of(maxInactivityInSeconds, ChronoUnit.SECONDS);
    }

    @Override
    public boolean stop()
    {
        synchronized (this)
        {
            if (m_watchdog != null)
            {
                m_watchdog.cancel(false);
                m_watchdog = null;
            }
        }

        return super.stop();
    }

    protected void resetWatchdog()
    {
        ensureWatchdog();

        m_expiration = TimeUtils.computeTimeoutExpiration(m_maxInactivity);
    }

    private synchronized void ensureWatchdog()
    {
        if (m_watchdog == null)
        {
            m_watchdog = Executors.scheduleOnDefaultPool(() ->
                                                         {
                                                             m_watchdog = null;

                                                             if (canContinue())
                                                             {
                                                                 if (TimeUtils.isTimeoutExpired(m_expiration))
                                                                 {
                                                                     reportError("Inactivity watchdog for '%s' fired!", getName());
                                                                     fireWatchdog();
                                                                 }

                                                                 ensureWatchdog();
                                                             }
                                                         }, m_maxInactivity.toNanos() * 2, TimeUnit.NANOSECONDS);
        }
    }

    protected abstract void fireWatchdog();
}
