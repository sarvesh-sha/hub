/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.argohytos;

import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.nio.file.ClosedFileSystemException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Maps;
import com.optio3.asyncawait.AsyncBackground;
import com.optio3.asyncawait.AsyncDelay;
import com.optio3.concurrency.Executors;
import com.optio3.infra.waypoint.FirmwareHelper;
import com.optio3.interop.mediaaccess.SerialAccess;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.protocol.common.ServiceWorker;
import com.optio3.protocol.common.ServiceWorkerWithWatchdog;
import com.optio3.protocol.model.ipn.objects.argohytos.stealthpower.ArgoHytos_LubCos;
import com.optio3.protocol.model.ipn.objects.argohytos.stealthpower.ArgoHytos_OPComII;
import com.optio3.protocol.model.ipn.objects.argohytos.stealthpower.BaseArgoHytosModel;
import com.optio3.util.BufferUtils;
import org.apache.commons.lang3.StringUtils;

public abstract class ArgoHytosManager implements AutoCloseable
{
    public static final Logger LoggerInstance = new Logger(ArgoHytosManager.class);

    private final String                              m_serialPort;
    private final ServiceWorker                       m_serialWorker;
    private       SerialAccess                        m_serialTransport;
    private       boolean                             m_lastRxFailed;
    private       Class<? extends BaseArgoHytosModel> m_detectedSensor;
    private       boolean                             m_detectedValue;

    private static class PartsParser
    {
        final Map<String, String> m_map = Maps.newHashMap();

        PartsParser(String line)
        {
            if (line.startsWith("$"))
            {
                byte crc = 0;

                for (int i = 0; i < line.length(); i++)
                {
                    crc += (byte) line.charAt(i);
                }

                crc += (byte) '\r';
                crc += (byte) '\n';
                if (crc == 0)
                {
                    for (var part : StringUtils.split(line.substring(1), ';'))
                    {
                        String[] key_value = StringUtils.split(part, ':');

                        if (key_value.length == 2)
                        {
                            m_map.put(key_value[0].trim(), key_value[1].trim());
                        }
                        else
                        {
                            m_map.put(part, "found");
                        }
                    }
                }
            }
        }

        int extractInt(String key)
        {
            String t = extractString(key, '[');

            try
            {
                return StringUtils.isNotEmpty(t) ? Integer.parseInt(t) : 0;
            }
            catch (NumberFormatException e)
            {
                return -1;
            }
        }

        float extractFloat(String key)
        {
            String t = extractString(key, '[');

            try
            {
                return StringUtils.isNotEmpty(t) ? Float.parseFloat(t) : 0.0f;
            }
            catch (NumberFormatException e)
            {
                return Float.NaN;
            }
        }

        String extractString(String key, char trimCharacter)
        {
            String t = extractString(key);

            int pos = t.indexOf(trimCharacter);
            if (pos >= 0)
            {
                t = t.substring(0, pos);
            }

            return t;
        }

        String extractString(String key)
        {
            return m_map.getOrDefault(key, "");
        }

        boolean isPresentt(String key)
        {
            return m_map.containsKey(key);
        }
    }

    public ArgoHytosManager(String port)
    {
        m_serialPort = port;

        m_serialWorker = new ServiceWorkerWithWatchdog(LoggerInstance, "ArgoHytos Serial", 60, 2000, 30)
        {
            @Override
            protected void shutdown()
            {
                closeTransport();
            }

            @Override
            protected void fireWatchdog()
            {
                reportError("ArgoHytos data flow stopped!");

                closeTransport();
            }

            @Override
            protected void worker()
            {
                final StringBuilder sb    = new StringBuilder();
                final byte[]        input = new byte[512];

                while (canContinue())
                {
                    SerialAccess transport = m_serialTransport;
                    if (transport == null)
                    {
                        try
                        {
                            FirmwareHelper f = FirmwareHelper.get();
                            f.selectPort(m_serialPort, FirmwareHelper.PortFlavor.RS232, false, false);

                            // This is weird. If we open the serial port once and change the speed, it doesn't stick. We have to close it and reopen it...
                            m_serialTransport = SerialAccess.openMultipleTimes(4, f.mapPort(m_serialPort), 9600, 8, 'N', 1);

                            notifyTransport(m_serialPort, true, false);
                            resetWatchdog();
                        }
                        catch (Throwable t)
                        {
                            reportFailure("Failed to start StealthPower serial", t);

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

                                if (len < 0)
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
                                                if (decode(sb.toString()))
                                                {
                                                    resetWatchdog();

                                                    reportErrorResolution("ArgoHytos data flow resumed!");
                                                }

                                                sb.setLength(0);
                                            }
                                            break;

                                        default:
                                            sb.append(c);
                                            break;
                                    }
                                }
                                m_lastRxFailed = false;

                                if (LoggerInstance.isEnabled(Severity.DebugObnoxious))
                                {
                                    BufferUtils.convertToHex(input, 0, len, 32, true, (line) -> LoggerInstance.debugObnoxious("RX: %s", line));
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

                            Severity level;

                            if (t instanceof ClosedFileSystemException)
                            {
                                // Expected, due to watchdog.
                                level = Severity.Debug;
                            }
                            else if (!m_lastRxFailed)
                            {
                                m_lastRxFailed = true;
                                level          = Severity.Info;
                            }
                            else
                            {
                                level = Severity.Debug;
                            }

                            LoggerInstance.log(null, level, null, null, "Got an exception trying to receive message: %s", t);

                            closeTransport();

                            workerSleep(10000);
                        }
                    }
                }
            }
        };
    }

    //--//

    @Override
    public void close() throws
                        Exception
    {
        m_serialWorker.close();
    }

    public void start()
    {
        m_serialWorker.start();
        m_detectedSensor = null;
    }

    public void stop()
    {
        m_detectedSensor = null;
        m_serialWorker.stop();
    }

    public synchronized void schedulePolling(int period)
    {
        if (m_serialWorker.canContinue())
        {
            // First time through, schedule immediately.
            executePolling(period, 0, TimeUnit.SECONDS);
        }
    }

    @AsyncBackground
    private CompletableFuture<Void> executePolling(int pollingPeriod,
                                                   @AsyncDelay long delay,
                                                   @AsyncDelay TimeUnit delayUnit)
    {
        if (m_serialWorker.canContinue())
        {
            try
            {
                if (m_detectedSensor == null)
                {
                    requestIdentification();
                }
                else
                {
                    requestValues();
                }
            }
            catch (Throwable t)
            {
            }

            if (m_serialWorker.canContinue())
            {
                executePolling(pollingPeriod, m_detectedSensor != null && m_detectedValue ? pollingPeriod : 1, TimeUnit.SECONDS);
            }
        }

        return wrapAsync(null);
    }

    //--//

    private void requestIdentification()
    {
        sendMessage("RID\r");
    }

    private void requestValues()
    {
        sendMessage("RVal\r");
    }

    private void sendMessage(String msg)
    {
        LoggerInstance.debugVerbose("Sending: %s", msg);

        SerialAccess serialTransport = m_serialTransport;
        if (serialTransport != null)
        {
            serialTransport.write(msg);

            Executors.safeSleep(20);
        }
    }

    //--//

    private synchronized void closeTransport()
    {
        if (m_serialTransport != null)
        {
            try
            {
                m_serialTransport.close();
            }
            catch (Throwable t)
            {
                // Ignore failures.
            }

            m_serialTransport = null;
        }

        notifyTransport(m_serialPort, false, true);
    }

    protected boolean decode(String line)
    {
        LoggerInstance.debugVerbose("Decode: %s", line);

        // $ARGO-HYTOS;OPCom II;SN:005308;SW:2.02.15;CRC:%
        if (line.startsWith("$"))
        {
            PartsParser parser = new PartsParser(line);

            if (m_detectedSensor == null)
            {
                if (parser.isPresentt("ARGO-HYTOS"))
                {
                    if (parser.isPresentt("LubCosH2O"))
                    {
                        m_detectedSensor = ArgoHytos_LubCos.class;
                        m_detectedValue  = false;
                        requestValues();
                        return true;
                    }

                    if (parser.isPresentt("OPCom II"))
                    {
                        m_detectedSensor = ArgoHytos_OPComII.class;
                        m_detectedValue  = false;
                        requestValues();
                        return true;
                    }
                }
            }
            else if (m_detectedSensor == ArgoHytos_LubCos.class)
            {
                var message = new ArgoHytos_LubCos();
                message.temperature       = parser.extractFloat("T");
                message.relative_humidity = parser.extractFloat("RH");

                m_detectedValue = true;
                receivedMessage(message);
            }
            else if (m_detectedSensor == ArgoHytos_OPComII.class)
            {
                var message = new ArgoHytos_OPComII();
                message.ISO4um  = parser.extractInt("ISO4um");
                message.ISO6um  = parser.extractInt("ISO6um");
                message.ISO14um = parser.extractInt("ISO14um");
                message.ISO21um = parser.extractInt("ISO21um");

                m_detectedValue = true;
                receivedMessage(message);
            }
        }

        return false;
    }

    //--//

    protected abstract void notifyTransport(String port,
                                            boolean opened,
                                            boolean closed);

    protected abstract void receivedMessage(BaseArgoHytosModel obj);
}
