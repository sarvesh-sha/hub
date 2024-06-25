/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.ipn;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.optio3.infra.waypoint.FirmwareHelper;
import com.optio3.interop.mediaaccess.SerialAccess;
import com.optio3.protocol.common.ServiceWorkerWithWatchdog;
import com.optio3.protocol.model.ipn.objects.IpnLocation;
import com.optio3.protocol.model.ipn.objects.montage.BaseBluetoothGatewayObjectModel;
import com.optio3.protocol.montage.BluetoothGatewayDecoder;

public final class WorkerForMontageBluetoothGateway extends ServiceWorkerWithWatchdog implements IpnWorker
{
    private final IpnManager   m_manager;
    private final String       m_port;
    private       SerialAccess m_transport;

    private final BluetoothGatewayDecoder m_decoder = new BluetoothGatewayDecoder()
    {
        @Override
        protected void receivedHeartbeat(Map<String, String> fields)
        {

        }

        @Override
        protected void receivedMessage(BaseBluetoothGatewayObjectModel obj)
        {
            m_manager.recordValue(obj);
        }

        @Override
        protected IpnLocation getLastLocation()
        {
            return m_manager.getState().getByClass(IpnLocation.class);
        }
    };

    public WorkerForMontageBluetoothGateway(IpnManager manager,
                                            String port)
    {
        super(IpnManager.LoggerInstance, "MontageBluetoothGateway", 60, 2000, 30);

        m_manager = manager;
        m_port    = port;
    }

    @Override
    public void startWorker()
    {
        start();
    }

    @Override
    public void stopWorker()
    {
        stop();
    }

    @Override
    public <T> T accessSubManager(Class<T> clz)
    {
        return null;
    }

    //--//

    @Override
    protected void shutdown()
    {
        closeTransport();
    }

    @Override
    protected void fireWatchdog()
    {
        closeTransport();
    }

    @Override
    protected void worker()
    {
        final StringBuilder sb    = new StringBuilder();
        final byte[]        input = new byte[512];

        while (canContinue())
        {
            SerialAccess transport = m_transport;
            if (transport == null)
            {
                try
                {
                    FirmwareHelper f = FirmwareHelper.get();

                    if (!f.mightBePresent(m_port, false))
                    {
                        reportError("Failed to start BluetoothGateway, device '%s' not found", m_port);

                        workerSleep(5000);
                        continue;
                    }

                    m_transport = new SerialAccess(f.mapPort(m_port), 115200, 8, 'N', 1);

                    m_manager.updateDiscoveryLatency(10, TimeUnit.SECONDS);

                    m_manager.notifyTransport(m_port, true, false);
                    reportErrorResolution("Reconnected to GPS!");
                    resetWatchdog();
                }
                catch (Throwable t)
                {
                    reportFailure("Failed to start GPS", t);

                    workerSleep(10000);
                }
            }
            else
            {
                try
                {
                    int len = transport.read(input, 1000);
                    if (len <= 0)
                    {
                        sb.setLength(0);

                        m_decoder.transitionToFailure();

                        if (m_decoder.shouldCloseTransport() || len < 0)
                        {
                            closeTransport();
                            workerSleep(500);
                        }
                    }
                    else
                    {
                        resetWatchdog();

                        for (int i = 0; i < len; i++)
                        {
                            char c = (char) input[i];

                            switch (c)
                            {
                                case '\n':
                                case '\r':
                                    if (sb.length() > 0)
                                    {
                                        m_decoder.process(sb.toString());
                                        sb.setLength(0);
                                    }
                                    break;

                                default:
                                    sb.append(c);
                                    break;
                            }
                        }
                    }
                }
                catch (Throwable t)
                {
                    if (!canContinue())
                    {
                        // The manager has been stopped, exit.
                        return;
                    }

                    closeTransport();

                    reportDebug("Received error: %s", t);

                    workerSleep(10000);
                }
            }
        }
    }

    private synchronized void closeTransport()
    {
        if (m_transport != null)
        {
            try
            {
                m_transport.close();
            }
            catch (Throwable t)
            {
                // Ignore failures.
            }

            m_transport = null;
        }

        m_manager.notifyTransport(m_port, false, true);
    }
}
