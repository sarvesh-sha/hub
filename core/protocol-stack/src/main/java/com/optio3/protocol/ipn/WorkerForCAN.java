/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.ipn;

import java.util.concurrent.TimeUnit;

import com.optio3.interop.mediaaccess.CanAccess;
import com.optio3.logging.Severity;
import com.optio3.protocol.can.CanManager;
import com.optio3.protocol.model.can.CanObjectModel;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;

public final class WorkerForCAN implements IpnWorker
{
    private final IpnManager m_manager;
    private final String     m_canPort;
    private final int        m_canFrequency;
    private final boolean    m_canNoTermination;
    private final boolean    m_canInvert;
    private       CanManager m_canManager;

    public WorkerForCAN(IpnManager manager,
                        String canPort,
                        int canFrequency,
                        boolean canNoTermination,
                        boolean canInvert)
    {
        m_manager = manager;
        m_canPort = canPort;
        m_canFrequency = canFrequency;
        m_canNoTermination = canNoTermination;
        m_canInvert = canInvert;
    }

    @Override
    public void startWorker()
    {
        m_canManager = new CanManager(m_canPort, m_canFrequency, m_canNoTermination, m_canInvert)
        {
            @Override
            protected void notifyGoodMessage(CanObjectModel val) throws
                                                                 Exception
            {
                if (CanManager.LoggerInstance.isEnabled(Severity.DebugVerbose))
                {
                    String json = ObjectMappers.prettyPrintAsJson(val);
                    CanManager.LoggerInstance.debugVerbose("Got CAN: %s\n", json);
                }

                m_manager.recordValue(val);
            }

            @Override
            protected void notifyUnknownMessage(CanAccess.BaseFrame frame)
            {
                if (CanManager.LoggerInstance.isEnabled(Severity.DebugVerbose))
                {
                    CanManager.LoggerInstance.info("Got unknown CAN message: %x\n", frame.encodeId());
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

        m_canManager.start();

        m_manager.updateDiscoveryLatency(15, TimeUnit.SECONDS);
    }

    @Override
    public void stopWorker() throws
                             Exception
    {
        if (m_canManager != null)
        {
            m_canManager.close();
            m_canManager = null;
        }
    }

    @Override
    public <T> T accessSubManager(Class<T> clz)
    {
        T mgr = Reflection.as(m_canManager, clz);
        if (mgr != null)
        {
            return mgr;
        }

        return null;
    }
}