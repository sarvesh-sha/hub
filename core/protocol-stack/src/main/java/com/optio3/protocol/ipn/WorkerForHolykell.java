/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.ipn;

import java.util.concurrent.TimeUnit;

import com.optio3.protocol.holykell.HolykellManager;
import com.optio3.serialization.Reflection;

public final class WorkerForHolykell implements IpnWorker
{
    private final IpnManager      m_manager;
    private final String          m_holykellPort;
    private final boolean         m_holykellInvert;
    private       HolykellManager m_holykellManager;

    public WorkerForHolykell(IpnManager manager,
                             String holykellPort,
                             boolean holykellInvert)
    {
        m_manager = manager;
        m_holykellPort = holykellPort;
        m_holykellInvert = holykellInvert;
    }

    @Override
    public void startWorker()
    {
        m_holykellManager = new HolykellManager(m_holykellPort, m_holykellInvert)
        {
            @Override
            protected void notifyTransport(String port,
                                           boolean opened,
                                           boolean closed)
            {
                m_manager.notifyTransport(port, opened, closed);
            }
        };

        m_holykellManager.start();

        m_holykellManager.schedulePolling(5, m_manager::recordValue);

        m_manager.updateDiscoveryLatency(6, TimeUnit.SECONDS);
    }

    @Override
    public void stopWorker() throws
                             Exception
    {
        if (m_holykellManager != null)
        {
            m_holykellManager.close();
            m_holykellManager = null;
        }
    }

    @Override
    public <T> T accessSubManager(Class<T> clz)
    {
        T mgr = Reflection.as(m_holykellManager, clz);
        if (mgr != null)
        {
            return mgr;
        }

        return null;
    }
}