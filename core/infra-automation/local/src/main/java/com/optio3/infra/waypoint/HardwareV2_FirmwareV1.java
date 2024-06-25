/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.waypoint;

import java.time.Duration;

import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.util.BoxingUtils;
import org.apache.commons.lang3.StringUtils;

class HardwareV2_FirmwareV1 extends FirmwareHelper
{
    private static final int   c_address        = 0x77;
    private static final float c_voltageScaling = 1.0f / 1000; // Millivolts
    private static final float c_voltageOffset  = 0.5f;
    private static final float c_delayScaling   = 1.0f / 10; // Tenths of seconds
    private static final float c_delayOffset    = 0;

    private final int    m_hardVer;
    private final int    m_softVer;
    private final String m_serialNumber;

    HardwareV2_FirmwareV1(int hardVer,
                          int softVer)
    {
        m_hardVer = hardVer;
        m_softVer = softVer;

        short serial01 = BoxingUtils.get(FirmwareCommands.Serial01.readWord(1, c_address), (short) 0);
        short serial23 = BoxingUtils.get(FirmwareCommands.Serial23.readWord(1, c_address), (short) 0);
        short serial45 = BoxingUtils.get(FirmwareCommands.Serial45.readWord(1, c_address), (short) 0);
        short serial67 = BoxingUtils.get(FirmwareCommands.Serial67.readWord(1, c_address), (short) 0);
        short serial89 = BoxingUtils.get(FirmwareCommands.Serial89.readWord(1, c_address), (short) 0);

        m_serialNumber = String.format("%04X%04X%04X%04X%04X", serial01, serial23, serial45, serial67, serial89);
    }

    static FirmwareHelper probe()
    {
        short hardVer = BoxingUtils.get(FirmwareCommands.VersionHardware.readWord(1, c_address), (short) 0);
        if (hardVer != 0)
        {
            LoggerInstance.info("Detected V%d.%02d hardware!", hardVer / 256, hardVer & 0xFF);
            switch (hardVer)
            {
                case 0x0200:
                    LoggerInstance.info("Detecting software...");
                    short softVer = BoxingUtils.get(FirmwareCommands.VersionFirmware.readWord(1, c_address), (short) 0);
                    LoggerInstance.info("Detected V%d.%02d software!", softVer / 256, softVer & 0xFF);
                    switch (softVer)
                    {
                        case 0x0102:
                            // VoltageReference is hardcoded.
                            return new HardwareV2_FirmwareV1(hardVer, softVer);

                        case 0x0103:
                            return new HardwareV2_FirmwareV1(hardVer, softVer);
                    }
            }
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
        return readTemperatureThroughSysFs();
    }

    @Override
    public float readBatteryVoltage()
    {
        return validateAndScaleFromDevice(FirmwareCommands.BatteryVoltage.readWord(1, c_address), c_voltageScaling, c_voltageOffset);
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

        cfg.turnOffVoltage = validateAndScaleFromDevice(FirmwareCommands.TurnOffVoltage.readWord(1, c_address), c_voltageScaling, c_voltageOffset);
        cfg.turnOnVoltage  = validateAndScaleFromDevice(FirmwareCommands.TurnOnVoltage.readWord(1, c_address), c_voltageScaling, c_voltageOffset);

        cfg.turnOffDelaySeconds = validateAndScaleFromDevice(FirmwareCommands.TurnOffDelay.readWord(1, c_address), c_delayScaling, c_delayOffset);
        cfg.turnOnDelaySeconds  = validateAndScaleFromDevice(FirmwareCommands.TurnOnDelay.readWord(1, c_address), c_delayScaling, c_delayOffset);

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

            // Disable system while we change the configuration.
            if (!FirmwareCommands.TurnOffVoltage.writeWord(1, c_address, 0))
            {
                return "Failed to disable configuration";
            }
        }

        boolean success;

        success = FirmwareCommands.TurnOffDelay.writeWord(1, c_address, scaleToDevice(cfg.turnOffDelaySeconds, c_delayScaling, c_delayOffset));
        success &= FirmwareCommands.TurnOnDelay.writeWord(1, c_address, scaleToDevice(cfg.turnOnDelaySeconds, c_delayScaling, c_delayOffset));

        success &= FirmwareCommands.TurnOnVoltage.writeWord(1, c_address, scaleToDevice(cfg.turnOnVoltage, c_voltageScaling, c_voltageOffset));
        success &= FirmwareCommands.TurnOffVoltage.writeWord(1, c_address, scaleToDevice(cfg.turnOffVoltage, c_voltageScaling, c_voltageOffset));

        return success ? null : "Failed to set configuration";
    }

    @Override
    public String getSerialNumber()
    {
        return m_serialNumber;
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
        if (parseCanPort(port) > 0)
        {
            if (invert)
            {
                // CAN swap not supported on this hardware.
                return false;
            }

            return true;
        }

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
        switch (flavor)
        {
            case RS232:
                if (isSharedSerial(port))
                {
                    FirmwareCommands.SerialRS232.writeByte(1, c_address, 1);
                    FirmwareCommands.SerialInvert.writeByte(1, c_address, 0);
                }
                break;

            case RS485:
                if (isSharedSerial(port))
                {
                    FirmwareCommands.SerialRS485.writeByte(1, c_address, 1);
                    FirmwareCommands.SerialInvert.writeByte(1, c_address, invert ? 1 : 0);
                }
                break;

            case CANbus:
                int portOffset = parseCanPort(port);
                if (portOffset != 0)
                {
                    if (termination)
                    {
                        FirmwareCommands.CanTerminationOn.writeByte(1, c_address, portOffset);
                    }
                    else
                    {
                        FirmwareCommands.CanTerminationOff.writeByte(1, c_address, portOffset);
                    }
                }
                break;
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
        FirmwareCommands.LedIntensityHigh.writeWord(1, c_address, (int) (intensityHigh * 255), led.getId());
        FirmwareCommands.LedIntensityLow.writeWord(1, c_address, (int) (intensityLow * 255), led.getId());

        FirmwareCommands.LedHoldHigh.writeWord(1, c_address, (int) (parseDuration(holdHigh) * 10), led.getId());
        FirmwareCommands.LedHoldLow.writeWord(1, c_address, (int) (parseDuration(holdLow) * 10), led.getId());

        float intensityDiff = Math.abs(intensityHigh - intensityLow);

        int rampUpStep   = 0;
        int rampDownStep = 0;

        float rampUpDuration = parseDuration(rampUp);
        if (rampUpDuration > 0)
        {
            float rampSlope = intensityDiff / rampUpDuration;

            rampUpStep = (int) (255 * rampSlope / 100.0f);
        }

        float rampDownDuration = parseDuration(rampDown);
        if (rampDownDuration > 0)
        {
            float rampSlope = intensityDiff / rampDownDuration;

            rampDownStep = (int) (255 * rampSlope / 100.0f);
        }

        FirmwareCommands.LedRampUp.writeWord(1, c_address, rampUpStep, led.getId());
        FirmwareCommands.LedRampDown.writeWord(1, c_address, rampDownStep, led.getId());

        FirmwareCommands.LedCycleCount.writeWord(1, c_address, cycleCount, led.getId());
    }

    //--//

    private boolean isSharedSerial(String port)
    {
        return StringUtils.equals(port, "/optio3-dev/optio3_RS485");
    }

    private boolean is12vSystem(float voltage)
    {
        return voltage < 20;
    }

    private boolean is24vSystem(float voltage)
    {
        return voltage < 30;
    }

    private static short scaleToDevice(float val,
                                       float scale,
                                       float offset)
    {
        return (Float.isNaN(val) || val == 0) ? 0 : (short) ((val - offset) / scale);
    }

    private static float validateAndScaleFromDevice(Short val,
                                                    float scale,
                                                    float offset)
    {
        return (val != null && val > 0) ? (val * scale) + offset : Float.NaN;
    }
}
