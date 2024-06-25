/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.ipn;

import java.util.concurrent.TimeUnit;

import com.optio3.infra.waypoint.FirmwareHelper;
import com.optio3.interop.mediaaccess.SerialAccess;
import com.optio3.protocol.common.ServiceWorkerWithWatchdog;
import com.optio3.protocol.model.ipn.objects.victron.VictronChargingMode;
import com.optio3.protocol.model.ipn.objects.victron.VictronErrorCode;
import com.optio3.protocol.model.ipn.objects.victron.Victron_RealTimeData;
import com.optio3.protocol.victron.VictronDecoder;
import com.optio3.util.Exceptions;
import org.apache.commons.lang3.StringUtils;

public final class WorkerForVictron extends ServiceWorkerWithWatchdog implements IpnWorker
{
    private final IpnManager   m_manager;
    private final String       m_victronPort;
    private       SerialAccess m_transport;

    private final VictronDecoder m_decoder = new VictronDecoder()
    {
        private final Victron_RealTimeData m_state = new Victron_RealTimeData();

        @Override
        protected boolean reportValue(String key,
                                      String value)
        {
            reportDebug("Key: %s = %s", key, value);

            switch (key)
            {
                case "V":
                    m_state.battery_voltage = parseNumber(value, Float.NaN);
                    break;

                case "I":
                    m_state.battery_current = parseNumber(value, Float.NaN);
                    break;

                case "T":
                    m_state.battery_temperature = parseNumber(value, Float.NaN);
                    break;

                case "VPV":
                    m_state.panel_voltage = parseNumber(value, Float.NaN);
                    break;

                case "PPV":
                    m_state.panel_power = parseNumber(value, Float.NaN);
                    break;

                case "IL":
                    m_state.load_current = parseNumber(value, Float.NaN);
                    break;

                case "CS":
                    m_state.charging_mode = VictronChargingMode.parse((int) parseNumber(value, -1));
                    break;

                case "ERR":
                    m_state.error_code = VictronErrorCode.parse((int) parseNumber(value, -1));
                    break;

                case "FW":
                    m_state.firmware_version = value;
                    break;

                case "PID":
                    m_state.product_id = value;
                    break;

                case "SER#":
                    m_state.serial_number = value;
                    break;

                default:
                    return false;
            }

            m_manager.recordValue(m_state);

            return true;
        }
    };

    public WorkerForVictron(IpnManager manager,
                            String victronPort)
    {
        super(IpnManager.LoggerInstance, "Victron", 60, 2000, 30);

        m_manager = manager;
        m_victronPort = victronPort;
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
                    f.selectPort(m_victronPort, FirmwareHelper.PortFlavor.RS232, false, false);

                    if (!f.mightBePresent(m_victronPort, false))
                    {
                        throw Exceptions.newRuntimeException("Device '%s' not found", m_victronPort);
                    }

                    m_transport = SerialAccess.openMultipleTimes(4, f.mapPort(m_victronPort), 19200, 8, 'N', 1);

                    m_manager.updateDiscoveryLatency(10, TimeUnit.SECONDS);

                    m_manager.notifyTransport(m_victronPort, true, false);
                    reportErrorResolution("Reconnected to Victron!");
                    resetWatchdog();
                }
                catch (Throwable t)
                {
                    reportFailure("Failed to start Victron", t);

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

        m_manager.notifyTransport(m_victronPort, false, true);
    }

    private static float parseNumber(String value,
                                     float defaultValue)
    {
        try
        {
            if (StringUtils.isNotBlank(value))
            {
                return Float.parseFloat(value);
            }
        }
        catch (NumberFormatException t)
        {
            // Ignore failure
        }

        return defaultValue;
    }
}