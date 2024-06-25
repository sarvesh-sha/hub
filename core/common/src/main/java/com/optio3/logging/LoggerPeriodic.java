/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.logging;

import java.util.concurrent.TimeUnit;

import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;

public abstract class LoggerPeriodic
{
    private final ILogger        m_logger;
    private final Severity       m_level;
    private final int            m_frequency;
    private final TimeUnit       m_unit;
    private       MonotonousTime m_lastReport;

    public LoggerPeriodic(ILogger logger,
                          Severity level,
                          int frequency,
                          TimeUnit unit)
    {
        m_logger = logger;
        m_level = level;
        m_frequency = frequency;
        m_unit = unit;
    }

    public void process()
    {
        if (m_logger.isEnabled(m_level))
        {
            if (TimeUtils.isTimeoutExpired(m_lastReport))
            {
                m_lastReport = TimeUtils.computeTimeoutExpiration(m_frequency, m_unit);

                onActivation();
            }
        }
        else
        {
            m_lastReport = null;
        }
    }

    protected abstract void onActivation();
}
