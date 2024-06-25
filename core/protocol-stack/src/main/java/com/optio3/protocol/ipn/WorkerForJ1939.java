/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.ipn;

import java.util.concurrent.TimeUnit;

import com.optio3.interop.mediaaccess.CanAccess;
import com.optio3.logging.Severity;
import com.optio3.protocol.model.obdii.ObdiiObjectModel;
import com.optio3.protocol.obdii.J1939Manager;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;

public final class WorkerForJ1939 implements IpnWorker
{
    private final IpnManager   m_manager;
    private final String       m_obdiiPort;
    private final int          m_obdiiFrequency;
    private final boolean      m_obdiiInvert;
    private       J1939Manager m_j1939Manager;

    public WorkerForJ1939(IpnManager manager,
                          String obdiiPort,
                          int obdiiFrequency,
                          boolean obdiiInvert)
    {
        m_manager = manager;
        m_obdiiPort = obdiiPort;
        m_obdiiFrequency = obdiiFrequency;
        m_obdiiInvert = obdiiInvert;
    }

    @Override
    public void startWorker()
    {
        m_j1939Manager = new J1939Manager(m_obdiiPort, m_obdiiFrequency, m_obdiiInvert)
        {
            @Override
            protected void notifyDecoded(ObdiiObjectModel obj) throws
                                                               Exception
            {
                if (J1939Manager.LoggerInstance.isEnabled(Severity.DebugVerbose))
                {
                    String json = ObjectMappers.prettyPrintAsJson(obj);
                    J1939Manager.LoggerInstance.debugVerbose("Got OBD-II: %s\n", json);
                }

                m_manager.recordValue(obj);
            }

            @Override
            protected void notifyNonDecoded(CanAccess.BaseFrame frame) throws
                                                                       Exception
            {
                if (J1939Manager.LoggerInstance.isEnabled(Severity.DebugVerbose))
                {
                    String json = ObjectMappers.prettyPrintAsJson(frame);
                    J1939Manager.LoggerInstance.debugVerbose("Got Unknown OBD-II: %s\n", json);
                }
            }

            @Override
            protected void notifyTransport(String port,
                                           boolean opened,
                                           boolean closed)
            {
                m_manager.notifyTransport(port, opened, closed);
            }
        };

        m_j1939Manager.start();

        m_manager.updateDiscoveryLatency(10, TimeUnit.SECONDS);
    }

    @Override
    public void stopWorker() throws
                             Exception
    {
        if (m_j1939Manager != null)
        {
            m_j1939Manager.close();
            m_j1939Manager = null;
        }
    }

    @Override
    public <T> T accessSubManager(Class<T> clz)
    {
        T mgr = Reflection.as(m_j1939Manager, clz);
        if (mgr != null)
        {
            return mgr;
        }

        return null;
    }
}