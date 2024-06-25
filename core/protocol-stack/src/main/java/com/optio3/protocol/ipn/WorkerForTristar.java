/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.ipn;

import java.util.concurrent.TimeUnit;

import com.optio3.protocol.tristar.TriStarManager;
import com.optio3.serialization.Reflection;

public final class WorkerForTristar implements IpnWorker
{
    private final IpnManager     m_manager;
    private final String         m_tristarPort;
    private       TriStarManager m_tristarManager;

    public WorkerForTristar(IpnManager manager,
                            String tristarPort)
    {
        m_manager = manager;
        m_tristarPort = tristarPort;
    }

    @Override
    public void startWorker()
    {
        m_tristarManager = new TriStarManager(m_tristarPort)
        {
            @Override
            protected void notifyTransport(String port,
                                           boolean opened,
                                           boolean closed)
            {
                m_manager.notifyTransport(port, opened, closed);
            }
        };

        m_tristarManager.start();

        m_tristarManager.schedulePolling(5, m_manager::recordValue);

        m_manager.updateDiscoveryLatency(6, TimeUnit.SECONDS);
    }

    @Override
    public void stopWorker() throws
                             Exception
    {
        if (m_tristarManager != null)
        {
            m_tristarManager.close();
            m_tristarManager = null;
        }
    }

    @Override
    public <T> T accessSubManager(Class<T> clz)
    {
        T mgr = Reflection.as(m_tristarManager, clz);
        if (mgr != null)
        {
            return mgr;
        }

        return null;
    }
}