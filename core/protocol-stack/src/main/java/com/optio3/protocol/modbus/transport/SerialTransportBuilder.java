/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.modbus.transport;

public final class SerialTransportBuilder
{
    private SerialTransport.Config m_config;

    private SerialTransportBuilder()
    {
    }

    public static SerialTransportBuilder newBuilder()
    {
        return new SerialTransportBuilder();
    }

    public SerialTransportBuilder setPort(String port)
    {
        ensureConfig().port = port;

        return this;
    }

    public SerialTransportBuilder setBaudRate(int baudRate)
    {
        ensureConfig().baudRate = baudRate;

        return this;
    }

    public SerialTransport build()
    {
        SerialTransport transport = new SerialTransport(ensureConfig());
        m_config = null;
        return transport;
    }

    //--//

    private SerialTransport.Config ensureConfig()
    {
        if (m_config == null)
        {
            m_config = new SerialTransport.Config();
        }

        return m_config;
    }
}
