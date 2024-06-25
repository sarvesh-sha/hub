/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.logging;

public class LoggerResource implements AutoCloseable
{
    private final PerThreadConfiguration m_config;

    LoggerResource(PerThreadConfiguration config)
    {
        m_config = config;
    }

    @Override
    public void close()
    {
        m_config.pop();
    }
}
