/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.ipn;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.optio3.protocol.model.ipn.objects.stealthpower.BaseStealthPowerModel;
import com.optio3.protocol.stealthpower.StealthPowerManager;
import com.optio3.serialization.Reflection;

public final class WorkerForStealthPower implements IpnWorker
{
    private final IpnManager          m_manager;
    private final String              m_stealthpowerPort;
    private       StealthPowerManager m_stealthpowerManager;

    public WorkerForStealthPower(IpnManager manager,
                                 String stealthpowerPort)
    {
        m_manager = manager;
        m_stealthpowerPort = stealthpowerPort;
    }

    @Override
    public void startWorker()
    {
        m_stealthpowerManager = new StealthPowerManager(m_stealthpowerPort)
        {
            @Override
            protected void notifyTransport(String port,
                                           boolean opened,
                                           boolean closed)
            {
                m_manager.notifyTransport(port, opened, closed);
            }

            @Override
            protected byte[] detectedBootloader(byte bootloadVersion,
                                                byte hardwareVersion,
                                                byte hardwareRevision)
            {
                try
                {
                    return m_manager.detectedStealthPowerBootloader(bootloadVersion, hardwareVersion, hardwareRevision);
                }
                catch (IOException e)
                {
                    LoggerInstance.error("Failed to get bootloader, due to %s", e);
                    return null;
                }
            }

            @Override
            protected void reportDownloadResult(int statusCode)
            {
                try
                {
                    m_manager.completedStealthPowerBootloader(statusCode);
                }
                catch (Throwable e)
                {
                    LoggerInstance.error("Failed to report bootloader result, due to %s", e);
                }
            }

            @Override
            protected void receivedMessage(BaseStealthPowerModel obj)
            {
                m_manager.recordValue(obj);
            }
        };

        m_stealthpowerManager.start();

        m_manager.updateDiscoveryLatency(6, TimeUnit.SECONDS);
    }

    @Override
    public void stopWorker() throws
                             Exception
    {
        if (m_stealthpowerManager != null)
        {
            m_stealthpowerManager.close();
            m_stealthpowerManager = null;
        }
    }

    @Override
    public <T> T accessSubManager(Class<T> clz)
    {
        T mgr = Reflection.as(m_stealthpowerManager, clz);
        if (mgr != null)
        {
            return mgr;
        }

        return null;
    }
}