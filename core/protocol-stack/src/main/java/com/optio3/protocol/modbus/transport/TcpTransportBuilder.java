/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.modbus.transport;

import com.google.common.base.Preconditions;

public final class TcpTransportBuilder
{
    private TcpTransport.Config m_config;

    private TcpTransportBuilder()
    {
    }

    public static TcpTransportBuilder newBuilder()
    {
        return new TcpTransportBuilder();
    }

    public TcpTransportBuilder setAddress(String address)
    {
        ensureConfig().address = address;

        return this;
    }

    public TcpTransportBuilder setPort(int port)
    {
        Preconditions.checkArgument(port >= 0 && port < 65536);

        ensureConfig().port = port;

        return this;
    }

    public TcpTransport build()
    {
        TcpTransport transport = new TcpTransport(ensureConfig());
        m_config = null;
        return transport;
    }

    //--//

    private TcpTransport.Config ensureConfig()
    {
        if (m_config == null)
        {
            m_config = new TcpTransport.Config();
        }

        return m_config;
    }
}
