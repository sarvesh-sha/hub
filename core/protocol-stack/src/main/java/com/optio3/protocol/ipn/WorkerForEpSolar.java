/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.ipn;

import java.util.concurrent.TimeUnit;

import com.optio3.protocol.epsolar.EpSolarManager;
import com.optio3.serialization.Reflection;

public final class WorkerForEpSolar implements IpnWorker
{
    private final IpnManager     m_manager;
    private final String         m_epsolarPort;
    private final boolean        m_epsolarInvert;
    private       EpSolarManager m_epsolarManager;

    public WorkerForEpSolar(IpnManager manager,
                            String epsolarPort,
                            boolean epsolarInvert)
    {
        m_manager = manager;
        m_epsolarPort = epsolarPort;
        m_epsolarInvert = epsolarInvert;
    }

    @Override
    public void startWorker()
    {
        m_epsolarManager = new EpSolarManager(m_epsolarPort, m_epsolarInvert)
        {
            @Override
            protected void notifyTransport(String port,
                                           boolean opened,
                                           boolean closed)
            {
                m_manager.notifyTransport(port, opened, closed);
            }
        };

        m_epsolarManager.start();

        m_epsolarManager.schedulePolling(5, m_manager::recordValue);

        m_manager.updateDiscoveryLatency(6, TimeUnit.SECONDS);
    }

    @Override
    public void stopWorker() throws
                             Exception
    {
        if (m_epsolarManager != null)
        {
            m_epsolarManager.close();
            m_epsolarManager = null;
        }
    }

    @Override
    public <T> T accessSubManager(Class<T> clz)
    {
        T mgr = Reflection.as(m_epsolarManager, clz);
        if (mgr != null)
        {
            return mgr;
        }

        return null;
    }
}