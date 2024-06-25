/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.modbus;

import com.google.common.base.Preconditions;
import com.optio3.protocol.modbus.transport.AbstractTransport;

public final class ModbusManagerBuilder
{
    private ModbusManager.Config m_config;

    private ModbusManagerBuilder()
    {
    }

    public static ModbusManagerBuilder newBuilder()
    {
        return new ModbusManagerBuilder();
    }

    public ModbusManagerBuilder setTransport(AbstractTransport transport)
    {
        ensureConfig().transport = transport;

        return this;
    }

    public ModbusManagerBuilder setMaxConcurrency(int maxParallelRequests)
    {
        Preconditions.checkArgument(maxParallelRequests > 0);

        ensureConfig().maxParallelRequests = maxParallelRequests;

        return this;
    }

    public ModbusManagerBuilder setDefaultRetries(int retries)
    {
        Preconditions.checkArgument(retries > 0);

        ensureConfig().defaultRetries = retries;

        return this;
    }

    public ModbusManagerBuilder setDefaultTimeout(int timeout)
    {
        Preconditions.checkArgument(timeout > 0);

        ensureConfig().defaultTimeout = timeout;

        return this;
    }

    public ModbusManager build()
    {
        ModbusManager mgr = new ModbusManager(ensureConfig());
        m_config = null;
        return mgr;
    }

    //--//

    private ModbusManager.Config ensureConfig()
    {
        if (m_config == null)
        {
            m_config = new ModbusManager.Config();
        }

        return m_config;
    }
}
