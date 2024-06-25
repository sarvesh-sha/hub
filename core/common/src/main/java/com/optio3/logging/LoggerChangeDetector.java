/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.logging;

public abstract class LoggerChangeDetector
{
    private final Logger   m_logger;
    private final Severity m_level;
    private       boolean  m_lastState;

    public LoggerChangeDetector(Logger logger,
                                Severity level)
    {
        m_logger = logger;
        m_level  = level;

        logger.registerChangeDetector(this);
    }

    public void process()
    {
        boolean state = m_logger.isEnabled(m_level);
        if (state != m_lastState)
        {
            m_lastState = state;

            try
            {
                if (state)
                {
                    onActivation();
                }
                else
                {
                    onDeactivation();
                }
            }
            catch (Exception e)
            {
                // Ignore failures.
            }
        }
    }

    protected abstract void onActivation();

    protected abstract void onDeactivation();
}
