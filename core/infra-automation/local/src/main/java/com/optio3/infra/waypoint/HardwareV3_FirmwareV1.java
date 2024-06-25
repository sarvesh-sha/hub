/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.waypoint;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.Maps;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.interop.mediaaccess.SerialAccess;
import com.optio3.util.MonotonousTime;
import com.optio3.util.Resources;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

class HardwareV3_FirmwareV1 extends FirmwareHelper
{
    static class PortMapping
    {
        final String     port;
        final PortFlavor flavor;
        final int        index;

        PortMapping(String port,
                    PortFlavor flavor,
                    int index)
        {
            this.port   = port;
            this.flavor = flavor;
            this.index  = index;
        }
    }

    private final static AtomicInteger            s_counter     = new AtomicInteger();
    private final        String                   m_serialNumber;
    private final        String                   m_boardName;
    private final        int                      m_hardVer;
    private final        int                      m_softVer;
    private final        Map<String, PortMapping> m_portMapping = Maps.newHashMap();
    private              KnownI2C                 m_supportsI2Cmultiplex;

    HardwareV3_FirmwareV1(String serialNumber,
                          String boardName,
                          int hardVer,
                          int softVer)
    {
        m_serialNumber = serialNumber;
        m_boardName    = boardName;
        m_hardVer      = hardVer;
        m_softVer      = softVer;

        m_portMapping.put("/optio3-dev/optio3_RS232", new PortMapping("/optio3-dev/ttySTM1", PortFlavor.RS232, 0));
        m_portMapping.put("/optio3-dev/optio3_RS232a", new PortMapping("/optio3-dev/ttySTM1", PortFlavor.RS232, 0));
        m_portMapping.put("/optio3-dev/optio3_RS232b", new PortMapping("/optio3-dev/ttySTM2", PortFlavor.RS232, 1));

        m_portMapping.put("/optio3-dev/optio3_RS485", new PortMapping("/optio3-dev/ttySTM3", PortFlavor.RS485, 0));
        m_portMapping.put("/optio3-dev/optio3_RS485a", new PortMapping("/optio3-dev/ttySTM3", PortFlavor.RS485, 0));
        m_portMapping.put("/optio3-dev/optio3_RS485b", new PortMapping("/optio3-dev/ttySTM4", PortFlavor.RS485, 1));

        m_portMapping.put("/dev/optio3_RS232", new PortMapping("/dev/ttySTM1", PortFlavor.RS232, 0));
        m_portMapping.put("/dev/optio3_RS232a", new PortMapping("/dev/ttySTM1", PortFlavor.RS232, 0));
        m_portMapping.put("/dev/optio3_RS232b", new PortMapping("/dev/ttySTM2", PortFlavor.RS232, 1));

        m_portMapping.put("/dev/optio3_RS485", new PortMapping("/dev/ttySTM3", PortFlavor.RS485, 0));
        m_portMapping.put("/dev/optio3_RS485a", new PortMapping("/dev/ttySTM3", PortFlavor.RS485, 0));
        m_portMapping.put("/dev/optio3_RS485b", new PortMapping("/dev/ttySTM4", PortFlavor.RS485, 1));

        m_portMapping.put("can0", new PortMapping("can0", PortFlavor.CANbus, 0));
        m_portMapping.put("can1", new PortMapping("can1", PortFlavor.CANbus, 1));
    }

    static FirmwareHelper probe()
    {
        try
        {
            boolean detected     = false;
            String  serialNumber = null;

            for (String line : Resources.loadLines("/proc/cpuinfo", false))
            {
                int pos = line.indexOf(':');
                if (pos > 0)
                {
                    String key   = line.substring(0, pos);
                    String value = line.substring(pos + 1);

                    key   = key.trim();
                    value = value.trim();
                    switch (key)
                    {
                        case "Hardware":
                            if (value.startsWith("STM32"))
                            {
                                detected = true;
                            }
                            break;

                        case "Serial":
                            serialNumber = value;
                            break;
                    }
                }
            }

            if (detected && serialNumber != null)
            {
                LoggerInstance.info("Detected STM32MP1: %s", serialNumber);
                String[] versions = exchangeCommand("*info", "");
                if (versions != null)
                {
                    return new HardwareV3_FirmwareV1(serialNumber, versions[0], Integer.parseInt(versions[1]), Integer.parseInt(versions[2]));
                }
            }
        }
        catch (Throwable t)
        {
            return null;
        }

        return null;
    }

    @Override
    public DockerImageArchitecture getArchitecture()
    {
        return DockerImageArchitecture.ARMv7;
    }

    @Override
    public int getHardwareVersion()
    {
        return m_hardVer;
    }

    @Override
    public int getFirmwareVersion()
    {
        return m_softVer;
    }

    @Override
    public float readTemperature()
    {
        String[] values = exchangeCommand("*adc", "");

        return extractFloatResult(values, 1);
    }

    @Override
    public float readBatteryVoltage()
    {
        String[] values = exchangeCommand("*adc", "");

        return extractFloatResult(values, 0) / 1000;
    }

    @Override
    public boolean supportsShutdownOnLowVoltage()
    {
        return true;
    }

    @Override
    public ShutdownConfiguration getShutdownConfiguration()
    {
        ShutdownConfiguration cfg = new ShutdownConfiguration();

        String[] results = exchangeCommand("*threshold", "");

        cfg.turnOffVoltage = extractFloatResult(results, 0);
        cfg.turnOnVoltage  = extractFloatResult(results, 1);

        cfg.turnOffDelaySeconds = extractFloatResult(results, 2);
        cfg.turnOnDelaySeconds  = extractFloatResult(results, 3);

        return cfg;
    }

    @Override
    public String setShutdownConfiguration(ShutdownConfiguration cfg)
    {
        if (Float.isNaN(cfg.turnOffVoltage) || cfg.turnOffVoltage == 0)
        {
            cfg.turnOffVoltage      = Float.NaN;
            cfg.turnOnVoltage       = Float.NaN;
            cfg.turnOffDelaySeconds = 0;
            cfg.turnOnDelaySeconds  = 0;
        }
        else
        {
            if (cfg.turnOffVoltage > cfg.turnOnVoltage)
            {
                return "Shutdown voltage cannot be lower than restart voltage";
            }

            float voltage = readBatteryVoltage();
            if (Float.isNaN(voltage))
            {
                // If we can't read the current voltage, don't try and set the configuration...
                return "Can't read current voltage";
            }

            float voltageWithMargin = voltage - 0.3f;
            if (cfg.turnOffVoltage > voltageWithMargin)
            {
                return "Not enough margin, configuration rejected";
            }

            if (is12vSystem(voltage))
            {
                if (cfg.turnOnVoltage > 12)
                {
                    return "Restart voltage too high, might not turn on again";
                }
            }
            else if (is24vSystem(voltage))
            {
                if (cfg.turnOnVoltage > 24)
                {
                    return "Restart voltage too high, might not turn on again";
                }
            }
        }

        String[] result = exchangeCommand("*threshold", "%f %f %d %d", cfg.turnOffVoltage, cfg.turnOnVoltage, cfg.turnOffDelaySeconds, cfg.turnOnDelaySeconds);
        return result != null ? null : "Failed to set configuration";
    }

    @Override
    public String getSerialNumber()
    {
        return m_serialNumber;
    }

    @Override
    public boolean supportsAccelerometer()
    {
        String[] values = exchangeCommand("*accelCheck", "");

        return extractBooleanResult(values, 0);
    }

    @Override
    public boolean configureAccelerometer(float frequency,
                                          float maxRangeInMillig)
    {
        String[] values = exchangeCommand("*accel", "%f %f", frequency, maxRangeInMillig);

        float resFrequency = extractFloatResult(values, 0) / 100;
        float resMaxRange  = extractFloatResult(values, 1);
        int   pending      = extractIntegerResult(values, 2, -1);
        long  tick         = extractUnsignedIntegerResult(values, 3, -1);

        return resFrequency > 0 && tick >= 0;
    }

    @Override
    public void streamAccelerometerSamples(AccelerometerCallback callback)
    {
        MonotonousTime timeout = TimeUtils.computeTimeoutExpiration(10, TimeUnit.MILLISECONDS);
        while (!TimeUtils.isTimeoutExpired(timeout))
        {
            String[] values = exchangeCommand("*accelSamples", "");
            if (values == null)
            {
                break;
            }

            double now = TimeUtils.nowMilliUtc();

            long tick = extractUnsignedIntegerResult(values, 0, -1);
            if (tick < 0)
            {
                break;
            }

            int pos = 1;
            while (true)
            {
                String sample = extractStringResult(values, pos++);
                if (sample == null)
                {
                    break;
                }

                String[] parts = StringUtils.split(sample, '/');
                if (parts.length != 4)
                {
                    break;
                }

                long sampleTick = extractUnsignedIntegerResult(parts, 0, -1);
                int  x          = extractIntegerResult(parts, 1, 0);
                int  y          = extractIntegerResult(parts, 2, 0);
                int  z          = extractIntegerResult(parts, 3, 0);

                // The MCU sends ticks as signed integers, but they are unsigned 32bit integers.
                // We returns them in longs. Do the subtraction and truncate to int, to get the correct difference.
                int tickDelta = (int) (sampleTick - tick);

                // Careful, don't use float, not enough resolution!!
                double timestamp = (now + tickDelta) * (1.0 / 1_000);

                LoggerInstance.debugVerbose("%,f : %d ,%d , %d", timestamp, x, y, z);

                callback.accept(timestamp, x, y, z);
            }
        }
    }

    //--//

    @Override
    public boolean supportsI2C()
    {
        return m_softVer >= 2;
    }

    @Override
    public boolean supportsI2Cmultiplex()
    {
        if (m_supportsI2Cmultiplex == null)
        {
            m_supportsI2Cmultiplex = KnownI2C.NONE;

            if (supportsI2C())
            {
                if (readI2C(-1, KnownI2C.PCA_9547.defaultAddress, 0, 0, 1) != null)
                {
                    m_supportsI2Cmultiplex = KnownI2C.PCA_9547;
                }
            }
        }

        return m_supportsI2Cmultiplex != KnownI2C.NONE;
    }

    @Override
    public int[] scanI2C(int bus)
    {
        selectBus(bus);

        String[] values = exchangeCommand("*i2cScan", "");
        if (values != null)
        {
            int[] res = new int[values.length];

            for (int i = 0; i < res.length; i++)
            {
                res[i] = extractIntegerResult(values, i, -1);
            }

            return res;
        }

        return null;
    }

    @Override
    public KnownI2C resolveI2C(int bus,
                               int address,
                               boolean noFiltering)
    {
        KnownI2C res = KnownI2C.resolveDefaultAddress(address);
        if (!noFiltering)
        {
            switch (res)
            {
                case PCA_9547: // Multiplexer, implicit
                    return KnownI2C.NONE;

                case MMA_8653: // Accelerometer, accessed through different API.
                    return KnownI2C.NONE;
            }
        }

        return res;
    }

    @Override
    public synchronized byte[] readI2C(int bus,
                                       int address,
                                       int regNum,
                                       int regSize,
                                       int length)
    {
        selectBus(bus);

        String[] values = exchangeCommand("*i2cRead", "%d %d %d %d", address, regNum, regSize, length);
        if (values != null)
        {
            byte[] res = new byte[values.length];

            for (int i = 0; i < res.length; i++)
            {
                res[i] = (byte) extractIntegerResult(values, i, -1);
            }

            return res;
        }

        return null;
    }

    @Override
    public synchronized boolean writeI2C(int bus,
                                         int address,
                                         int regNum,
                                         int regSize,
                                         byte[] payload)
    {
        selectBus(bus);

        StringBuilder payloadText = new StringBuilder();
        for (byte b : payload)
        {
            if (payloadText.length() > 0)
            {
                payloadText.append(" ");
            }

            payloadText.append(b & 0xFF);
        }

        String[] values = exchangeCommand("*i2cWrite", "%d %d %d %d %s", address, regNum, regSize, payload.length, payloadText);
        return values != null;
    }

    private void selectBus(int bus)
    {
        if (bus >= 0 && supportsI2Cmultiplex())
        {
            switch (m_supportsI2Cmultiplex)
            {
                case PCA_9547:
                    if (bus < 8)
                    {
                        bus |= 8;
                    }
                    else
                    {
                        bus = 0;
                    }

                    writeI2C(-1, KnownI2C.PCA_9547.defaultAddress, 0, 0, new byte[] { (byte) bus });
                    break;
            }
        }
    }

    //--//

    @Override
    public boolean mightBePresent(String port,
                                  boolean invert)
    {
        if (parseCanPort(port) > 0)
        {
            return true;
        }

        return portFileExists(port) || portFileExists(mapPort(port));
    }

    @Override
    public String mapPort(String port)
    {
        PortMapping mapping = m_portMapping.get(port);
        return mapping != null ? mapping.port : port;
    }

    @Override
    public void selectPort(String port,
                           PortFlavor flavor,
                           boolean invert,
                           boolean termination)
    {
        PortMapping mapping = m_portMapping.get(port);
        if (mapping != null && mapping.flavor == flavor)
        {
            switch (flavor)
            {
                case RS232:
                    break;

                case RS485:
                    exchangeCommand("*rs485", "%d %d", mapping.index, invert ? 1 : 0);
                    break;

                case CANbus:
                    exchangeCommand("*can", "%d %d %d", mapping.index, invert ? 1 : 0, termination ? 1 : 0);
                    break;
            }
        }
    }

    @Override
    public void setLed(Led led,
                       float intensityHigh,
                       float intensityLow,
                       Duration holdHigh,
                       Duration holdLow,
                       Duration rampUp,
                       Duration rampDown,
                       int cycleCount)
    {
        switch (led)
        {
            case Red:
            case Green:
            case Blue:
                exchangeCommand("*led",
                                "%s %f %f %f %f %f %f %d",
                                led.getName(),
                                intensityHigh,
                                intensityLow,
                                parseDuration(holdHigh),
                                parseDuration(holdLow),
                                parseDuration(rampUp),
                                parseDuration(rampDown),
                                cycleCount);
                break;
        }
    }

    //--//

    private boolean is12vSystem(float voltage)
    {
        return voltage < 20;
    }

    private boolean is24vSystem(float voltage)
    {
        return voltage < 30;
    }

    private static String[] exchangeCommand(String cmd,
                                            String format,
                                            Object... args)
    {
        synchronized (s_counter)
        {
            try
            {
                final String virtualUART = "/optio3-dev/ttyRPMSG0";

                try (SerialAccess port = new SerialAccess(virtualUART, true))
                {
                    String cmdAndSeq = String.format("%s/%d", cmd, s_counter.incrementAndGet());

                    String outCmd = String.format("%s %s\n", cmdAndSeq, String.format(format, args));
                    LoggerInstance.debug("Sent: %s", outCmd);

                    port.write(outCmd);

                    byte[] buf           = new byte[1024];
                    byte[] bufLine       = new byte[1024];
                    int    bufLineOffset = 0;

                    while (bufLineOffset < bufLine.length)
                    {
                        int read = port.read(buf, 100);
                        if (read <= 0)
                        {
                            break;
                        }

                        for (int i = 0; i < read; i++)
                        {
                            byte c = buf[i];

                            if (c == '\n')
                            {
                                String line = new String(bufLine, 0, bufLineOffset);

                                LoggerInstance.debug("Received: %s", line);
                                String[] parts = StringUtils.split(line, ' ');
                                if (parts.length >= 2)
                                {
                                    if (StringUtils.equals(cmdAndSeq, parts[1]) && StringUtils.equals("SUCCESS", parts[0]))
                                    {
                                        return Arrays.copyOfRange(parts, 2, parts.length);
                                    }
                                }

                                return null;
                            }

                            bufLine[bufLineOffset++] = c;
                        }
                    }

                    return null;
                }
            }
            catch (Throwable t)
            {
                LoggerInstance.debug("Failed due to: %s", t);
                return null;
            }
        }
    }

    private static String extractStringResult(String[] results,
                                              int i)
    {
        if (results == null || i >= results.length)
        {
            return null;
        }

        return results[i];
    }

    private static int extractIntegerResult(String[] results,
                                            int i,
                                            int defaultValue)
    {
        String text = extractStringResult(results, i);

        try
        {
            return text != null ? Integer.parseInt(text) : defaultValue;
        }
        catch (Throwable t)
        {
            LoggerInstance.debugObnoxious("extractIntegerResult for '%s' failed, due to %s", text, t);
            return defaultValue;
        }
    }

    private static long extractUnsignedIntegerResult(String[] results,
                                                     int i,
                                                     long defaultValue)
    {
        String text = extractStringResult(results, i);

        try
        {
            return text != null ? (Long.parseLong(text) & 0xFFFF_FFFFL) : defaultValue;
        }
        catch (Throwable t)
        {
            LoggerInstance.debugObnoxious("extractUnsignedIntegerResult for '%s' failed, due to %s", text, t);
            return defaultValue;
        }
    }

    private static float extractFloatResult(String[] results,
                                            int i)
    {
        String text = extractStringResult(results, i);

        try
        {
            return text != null ? Float.parseFloat(text) : Float.NaN;
        }
        catch (Throwable t)
        {
            LoggerInstance.debugObnoxious("extractFloatResult for '%s' failed, due to %s", text, t);
            return Float.NaN;
        }
    }

    private static boolean extractBooleanResult(String[] results,
                                                int i)
    {
        String text = extractStringResult(results, i);

        try
        {
            return Boolean.parseBoolean(text);
        }
        catch (Throwable t)
        {
            LoggerInstance.debugObnoxious("extractBooleanResult for '%s' failed, due to %s", text, t);
            return false;
        }
    }
}
