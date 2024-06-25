/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.ipn;

import java.util.concurrent.TimeUnit;

import com.optio3.protocol.argohytos.ArgoHytosManager;
import com.optio3.protocol.model.ipn.objects.argohytos.stealthpower.BaseArgoHytosModel;
import com.optio3.serialization.Reflection;

public final class WorkerForArgoHytos implements IpnWorker
{
    private final IpnManager       m_manager;
    private final String           m_argoHytosPort;
    private       ArgoHytosManager m_argoHytosManager;

    public WorkerForArgoHytos(IpnManager manager,
                              String argoHytosPort)
    {
        m_manager       = manager;
        m_argoHytosPort = argoHytosPort;
    }

    @Override
    public void startWorker()
    {
        m_argoHytosManager = new ArgoHytosManager(m_argoHytosPort)
        {
            @Override
            protected void notifyTransport(String port,
                                           boolean opened,
                                           boolean closed)
            {
                m_manager.notifyTransport(port, opened, closed);
            }

            @Override
            protected void receivedMessage(BaseArgoHytosModel obj)
            {
                m_manager.recordValue(obj);
            }
        };

        m_argoHytosManager.start();

        m_argoHytosManager.schedulePolling(30);

        m_manager.updateDiscoveryLatency(6, TimeUnit.SECONDS);
    }

    @Override
    public void stopWorker() throws
                             Exception
    {
        if (m_argoHytosManager != null)
        {
            m_argoHytosManager.close();
            m_argoHytosManager = null;
        }
    }

    @Override
    public <T> T accessSubManager(Class<T> clz)
    {
        T mgr = Reflection.as(m_argoHytosManager, clz);
        if (mgr != null)
        {
            return mgr;
        }

        return null;
    }
}
