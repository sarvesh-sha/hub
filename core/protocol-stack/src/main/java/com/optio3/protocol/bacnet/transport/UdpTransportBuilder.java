/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.transport;

import com.google.common.base.Preconditions;

public final class UdpTransportBuilder
{
    private UdpTransport.Config m_config;

    private UdpTransportBuilder()
    {
    }

    public static UdpTransportBuilder newBuilder()
    {
        return new UdpTransportBuilder();
    }

    public UdpTransportBuilder setDevice(String device)
    {
        ensureConfig().device = device;

        return this;
    }

    public UdpTransportBuilder setServerPort(int port)
    {
        Preconditions.checkArgument(port >= 0 && port < 65536);

        ensureConfig().serverPort = port;

        return this;
    }

    public UdpTransportBuilder setNetworkPort(int port)
    {
        Preconditions.checkArgument(port >= 0 && port < 65536);

        ensureConfig().networkPort = port;

        return this;
    }

    public UdpTransport build()
    {
        UdpTransport transport = new UdpTransport(ensureConfig());
        m_config = null;
        return transport;
    }

    //--//

    private UdpTransport.Config ensureConfig()
    {
        if (m_config == null)
        {
            m_config = new UdpTransport.Config();
        }

        return m_config;
    }
}
