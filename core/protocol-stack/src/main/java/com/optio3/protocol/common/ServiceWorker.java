/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.common;

import java.lang.management.ThreadInfo;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.optio3.concurrency.Executors;
import com.optio3.logging.ILogger;
import com.optio3.logging.LoggerFactory;
import com.optio3.util.StackTraceAnalyzer;

public abstract class ServiceWorker implements AutoCloseable
{
    private final ILogger m_logger;
    private final String  m_name;
    private final int     m_failureDelay;
    private final int     m_stopMaxWait;

    private Thread             m_worker;
    private ScheduledFuture<?> m_delayedWarning;
    private boolean            m_warnedOfFailure;

    protected ServiceWorker(ILogger logger,
                            String name,
                            int failureDelayInSeconds,
                            int stopMaxWait)
    {
        m_logger = logger;
        m_name = name;
        m_failureDelay = failureDelayInSeconds;
        m_stopMaxWait = stopMaxWait;
    }

    @Override
    public void close()
    {
        stop();
    }

    public String getName()
    {
        return m_name;
    }

    public boolean start()
    {
        try
        {
            Thread worker = new Thread(this::worker);
            worker.setDaemon(true);
            worker.setName(m_name);
            worker.start();
            m_worker = worker;
            return true;
        }
        catch (Throwable t)
        {
            reportErrorDirect("Failed to start %s, due to %s", m_name, t);
            return false;
        }
    }

    public boolean stop()
    {
        dismissError();

        Thread worker = m_worker;
        if (worker != null)
        {
            m_worker = null;

            shutdown();

            try
            {
                worker.interrupt();
                worker.join(m_stopMaxWait);
            }
            catch (InterruptedException e)
            {
                // Ignore interruptions.
            }

            if (worker.isAlive())
            {
                reportErrorDirect("%s worker failed to shutdown in a timely manner...", m_name);

                Map<StackTraceAnalyzer, List<ThreadInfo>> uniqueStackTraces = StackTraceAnalyzer.allThreadInfos();
                List<String>                              lines             = StackTraceAnalyzer.printThreadInfos(true, uniqueStackTraces);

                for (String line : lines)
                {
                    reportErrorDirect("%s: %s", m_name, line);
                }

                return false;
            }
        }

        return true;
    }

    public boolean canContinue()
    {
        return m_worker != null;
    }

    protected abstract void worker();

    protected abstract void shutdown();

    protected void reportFailure(String prefix,
                                 Throwable t)
    {
        reportError("%s, due to %s", prefix, LoggerFactory.extractExceptionDescription(t));
    }

    protected synchronized void reportError(String fmt,
                                            Object... args)
    {
        if (!m_warnedOfFailure && m_delayedWarning == null)
        {
            m_delayedWarning = Executors.scheduleOnDefaultPool(() ->
                                                               {
                                                                   m_warnedOfFailure = true;
                                                                   reportErrorDirect(fmt, args);
                                                               }, m_failureDelay, TimeUnit.SECONDS);
        }
    }

    protected synchronized void reportErrorResolution(String fmt,
                                                      Object... args)
    {
        dismissError();

        if (m_warnedOfFailure)
        {
            m_logger.info(fmt, args);
            m_warnedOfFailure = false;
        }
    }

    private synchronized void dismissError()
    {
        if (m_delayedWarning != null)
        {
            m_delayedWarning.cancel(false);
            m_delayedWarning = null;
        }
    }

    private void reportErrorDirect(String fmt,
                                   Object... args)
    {
        m_logger.error(fmt, args);
    }

    protected void reportDebug(String fmt,
                               Object... args)
    {
        m_logger.debug(fmt, args);
    }

    protected void workerSleep(int milliSec)
    {
        while (milliSec > 0 && canContinue())
        {
            int wait = Math.min(milliSec, 200);
            Executors.safeSleep(wait);
            milliSec -= wait;
        }
    }
}
