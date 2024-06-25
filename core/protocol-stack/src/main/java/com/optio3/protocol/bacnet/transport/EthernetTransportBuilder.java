/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.transport;

public final class EthernetTransportBuilder
{
    private EthernetTransport.Config m_config;

    private EthernetTransportBuilder()
    {
    }

    public static EthernetTransportBuilder newBuilder()
    {
        return new EthernetTransportBuilder();
    }

    public EthernetTransportBuilder setDevice(String device)
    {
        ensureConfig().device = device;

        return this;
    }

    public EthernetTransport build()
    {
        EthernetTransport transport = new EthernetTransport(ensureConfig());
        m_config = null;
        return transport;
    }

    //--//

    private EthernetTransport.Config ensureConfig()
    {
        if (m_config == null)
        {
            m_config = new EthernetTransport.Config();
        }

        return m_config;
    }
}
