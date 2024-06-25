/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.waypoint;

import java.io.File;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.logging.Logger;
import com.sun.jna.Platform;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

public abstract class FirmwareHelper
{
    public static final Logger LoggerInstance = new Logger(FirmwareHelper.class);

    private static final Supplier<FirmwareHelper> s_instance = Suppliers.memoize(() ->
                                                                                 {
                                                                                     FirmwareHelper fh;

                                                                                     LoggerInstance.info("Detecting hardware...");

                                                                                     fh = HardwareV2_FirmwareV1.probe();
                                                                                     if (fh == null)
                                                                                     {
                                                                                         fh = HardwareV3_FirmwareV1.probe();
                                                                                     }

                                                                                     if (fh == null)
                                                                                     {
                                                                                         LoggerInstance.info("No custom hardware detected...");
                                                                                         fh = new Noop();
                                                                                     }

                                                                                     return fh;
                                                                                 });

    //--//

    public static class ShutdownConfiguration
    {
        public float turnOffVoltage;
        public float turnOnVoltage;

        public float turnOffDelaySeconds;
        public float turnOnDelaySeconds;
    }

    public enum PortFlavor
    {
        RS232,
        RS485,
        CANbus
    }

    @FunctionalInterface
    public interface AccelerometerCallback
    {
        void accept(double timestamp,
                    int x,
                    int y,
                    int z);
    }

    //--//

    private static class Noop extends FirmwareHelper
    {
        @Override
        public DockerImageArchitecture getArchitecture()
        {
            return DockerImageArchitecture.parse(Platform.ARCH);
        }

        @Override
        public int getHardwareVersion()
        {
            return 0;
        }

        @Override
        public int getFirmwareVersion()
        {
            return 0;
        }

        @Override
        public String getSerialNumber()
        {
            return null;
        }

        @Override
        public float readTemperature()
        {
            return readTemperatureThroughSysFs();
        }

        @Override
        public float readBatteryVoltage()
        {
            return Float.NaN;
        }

        @Override
        public boolean supportsShutdownOnLowVoltage()
        {
            return false;
        }

        @Override
        public ShutdownConfiguration getShutdownConfiguration()
        {
            return null;
        }

        @Override
        public String setShutdownConfiguration(ShutdownConfiguration cfg)
        {
            return "Not supported";
        }

        @Override
        public boolean supportsAccelerometer()
        {
            return false;
        }

        @Override
        public boolean configureAccelerometer(float frequency,
                                              float maxRangeInMillig)
        {
            return false;
        }

        @Override
        public void streamAccelerometerSamples(AccelerometerCallback callback)
        {
        }

        @Override
        public boolean supportsI2C()
        {
            return false;
        }

        @Override
        public boolean supportsI2Cmultiplex()
        {
            return false;
        }

        @Override
        public int[] scanI2C(int bus)
        {
            return null;
        }

        @Override
        public KnownI2C resolveI2C(int bus,
                                   int address,
                                   boolean noFiltering)
        {
            return KnownI2C.NONE;
        }

        @Override
        public byte[] readI2C(int bus,
                              int address,
                              int regNum,
                              int regSize,
                              int length)
        {
            return null;
        }

        @Override
        public boolean writeI2C(int bus,
                                int address,
                                int regNum,
                                int regSize,
                                byte[] payload)
        {
            return false;
        }

        @Override
        public boolean mightBePresent(String port,
                                      boolean invert)
        {
            return portFileExists(port);
        }

        @Override
        public String mapPort(String port)
        {
            return port;
        }

        @Override
        public void selectPort(String port,
                               PortFlavor flavor,
                               boolean invert,
                               boolean termination)
        {
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
        }
    }

    //--//

    public static FirmwareHelper get()
    {
        return s_instance.get();
    }

    public static DockerImageArchitecture architecture()
    {
        return get().getArchitecture();
    }

    public abstract DockerImageArchitecture getArchitecture();

    public abstract int getHardwareVersion();

    public abstract int getFirmwareVersion();

    public abstract String getSerialNumber();

    //--//

    public abstract float readTemperature();

    public abstract float readBatteryVoltage();

    //--//

    public abstract boolean supportsShutdownOnLowVoltage();

    public abstract ShutdownConfiguration getShutdownConfiguration();

    public abstract String setShutdownConfiguration(ShutdownConfiguration cfg);

    //--//

    public abstract boolean supportsAccelerometer();

    public abstract boolean configureAccelerometer(float frequency,
                                                   float maxRangeInMillig);

    public abstract void streamAccelerometerSamples(AccelerometerCallback callback);

    //--//

    public abstract boolean supportsI2C();

    public abstract boolean supportsI2Cmultiplex();

    public abstract int[] scanI2C(int bus);

    public abstract KnownI2C resolveI2C(int bus,
                                        int address,
                                        boolean noFiltering);

    public abstract byte[] readI2C(int bus,
                                   int address,
                                   int regNum,
                                   int regSize,
                                   int length);

    public abstract boolean writeI2C(int bus,
                                     int address,
                                     int regNum,
                                     int regSize,
                                     byte[] payload);

    //--//

    public abstract boolean mightBePresent(String port,
                                           boolean invert);

    public abstract String mapPort(String port);

    public abstract void selectPort(String port,
                                    PortFlavor flavor,
                                    boolean invert,
                                    boolean termination);

    public abstract void setLed(Led led,
                                float intensityHigh,
                                float intensityLow,
                                Duration holdHigh,
                                Duration holdLow,
                                Duration rampUp,
                                Duration rampDown,
                                int cycleCount);

    public void turnLedOff(Led led)
    {
        turnLedOn(led, 0);
    }

    public void turnLedOn(Led led,
                          float intensity)
    {
        setLed(led, intensity, 0, null, null, null, null, 0);
    }

    public void blinkLed(Led led,
                         float intensityHigh,
                         Duration cycleTime,
                         float dutyCycle)
    {
        dutyCycle = Math.max(0, dutyCycle);
        dutyCycle = Math.min(1, dutyCycle);

        setLed(led, intensityHigh, 0, convertToFraction(cycleTime, dutyCycle), convertToFraction(cycleTime, 1 - dutyCycle), null, null, 0);
    }

    public void rampLed(Led led,
                        float intensityHigh,
                        Duration cycleTime,
                        float dutyCycle)
    {
        dutyCycle = Math.max(0, dutyCycle);
        dutyCycle = Math.min(1, dutyCycle);

        setLed(led, intensityHigh, 0, null, null, convertToFraction(cycleTime, dutyCycle), convertToFraction(cycleTime, 1 - dutyCycle), 0);
    }

    //--//

    protected static float parseDuration(Duration duration)
    {
        if (duration == null)
        {
            return 0;
        }

        return duration.getSeconds() + (duration.getNano() / 1E9f);
    }

    protected static Duration convertToFraction(Duration duration,
                                                float ratio)
    {
        return Duration.ofNanos((long) (parseDuration(duration) * 1E9 * ratio));
    }

    protected boolean portFileExists(String port)
    {
        return new File(port).exists();
    }

    protected static int parseCanPort(String port)
    {
        if (StringUtils.equals(port, "can0"))
        {
            return 1;
        }

        if (StringUtils.equals(port, "can1"))
        {
            return 2;
        }

        return 0;
    }

    //--//

    protected float readTemperatureThroughSysFs()
    {
        try
        {
            File file = new File("/sys/class/thermal/thermal_zone0/temp");
            if (file.exists())
            {
                String text = FileUtils.readFileToString(file, Charset.defaultCharset());
                return Float.parseFloat(text) / 1000;
            }
        }
        catch (Throwable t)
        {
            // Ignore failures for temperature
        }

        return Float.NaN;
    }
}
