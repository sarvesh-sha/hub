/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.util;

import java.lang.management.ThreadInfo;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.optio3.concurrency.Executors;
import com.optio3.logging.Logger;

public class StuckComputationChecker implements AutoCloseable
{
    public static final Logger LoggerInstance = new Logger(StuckComputationChecker.class);

    private final String             m_identifier;
    private final ScheduledFuture<?> m_timeout;
    private final long               m_duration;

    public StuckComputationChecker(String identifier,
                                   long delay,
                                   TimeUnit unit)
    {
        m_identifier = identifier;
        m_duration = TimeUnit.MICROSECONDS.convert(delay, unit);

        m_timeout = Executors.scheduleOnDefaultPool(this::timeout, delay, unit);
    }

    @Override
    public void close()
    {
        m_timeout.cancel(false);
    }

    private void timeout()
    {
        if (m_duration >= 1_000_000)
        {
            LoggerInstance.warn("Detected a stuck computation: %s (took longer than %d sec(s)", m_identifier, m_duration / 1_000_000);
        }
        else if (m_duration >= 1000)
        {
            LoggerInstance.warn("Detected a stuck computation: %s (took longer than %d millisec(s)", m_identifier, m_duration / 1000);
        }
        else
        {
            LoggerInstance.warn("Detected a stuck computation: %s (took longer than %d usec(s)", m_identifier, m_duration);
        }

        Map<StackTraceAnalyzer, List<ThreadInfo>> uniqueStackTraces = StackTraceAnalyzer.allThreadInfos();
        List<String>                              lines             = StackTraceAnalyzer.printThreadInfos(false, uniqueStackTraces);
        for (String line : lines)
        {
            LoggerInstance.warn(line);
        }
    }
}
