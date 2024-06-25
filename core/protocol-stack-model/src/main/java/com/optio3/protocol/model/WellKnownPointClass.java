/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import com.optio3.cloud.model.IEnumDescription;
import org.apache.commons.lang3.StringUtils;

public enum WellKnownPointClass implements IEnumDescription
{
    // @formatter:off
    None                                (0     , null                                    ),
    Log                                 (0x0001, "Log Entry"         , WellKnownTag.Log  ),
    Ignored                             (0x0002, "Ignored"                               ),
    //--//
    LocationLongitude                   (0x1001, "Location Longitude", WellKnownTag.Location, WellKnownTag.Longitude),
    LocationLatitude                    (0x1002, "Location Latitude" , WellKnownTag.Location, WellKnownTag.Latitude ),
    LocationSpeed                       (0x1003, "Location Speed"    , WellKnownTag.Location, WellKnownTag.Velocity ),
    LocationAltitude                    (0x1004, "Location Altitude" , WellKnownTag.Location, WellKnownTag.Altitude ),
    LocationHeading                     (0x1005, "Location Heading"  , WellKnownTag.Location, WellKnownTag.Heading  ),
    //--//
    AccelerationX                       (0x1101, "Acceleration X"    , WellKnownTag.Acceleration, WellKnownTag.AxisX),
    AccelerationY                       (0x1102, "Acceleration Y"    , WellKnownTag.Acceleration, WellKnownTag.AxisY),
    AccelerationZ                       (0x1103, "Acceleration Z"    , WellKnownTag.Acceleration, WellKnownTag.AxisZ),
    Acceleration                        (0x1104, "Acceleration"      , WellKnownTag.Acceleration, WellKnownTag.Total),
    VelocityX                           (0x1105, "Velocity X"        , WellKnownTag.Velocity    , WellKnownTag.AxisX),
    VelocityY                           (0x1106, "Velocity Y"        , WellKnownTag.Velocity    , WellKnownTag.AxisY),
    VelocityZ                           (0x1107, "Velocity Z"        , WellKnownTag.Velocity    , WellKnownTag.AxisZ),
    Velocity                            (0x1108, "Velocity"          , WellKnownTag.Velocity    , WellKnownTag.Total),
    //--//
    ArrayVoltage                        (0x2001, "Array Voltage", WellKnownTag.SolarPanel, WellKnownTag.Voltage, WellKnownTag.Production),
    ArrayCurrent                        (0x2002, "Array Current", WellKnownTag.SolarPanel, WellKnownTag.Current, WellKnownTag.Production),
    ArrayPower                          (0x2003, "Array Power"  , WellKnownTag.SolarPanel, WellKnownTag.Power  , WellKnownTag.Production),
    //--//
    BatteryVoltage                      (0x3001, "Battery Voltage"        , WellKnownTag.Battery, WellKnownTag.Voltage      ),
    BatteryCurrent                      (0x3002, "Battery Current"        , WellKnownTag.Battery, WellKnownTag.Current      ),
    BatteryPower                        (0x3003, "Battery Power"          , WellKnownTag.Battery, WellKnownTag.Power        ),
    BatteryStateOfCharge                (0x3004, "Battery State Of Charge", WellKnownTag.Battery, WellKnownTag.StateOfCharge),
    BatteryTemperature                  (0x3005, "Battery Temperature"    , WellKnownTag.Battery, WellKnownTag.Temperature  ),
    ExternalVoltage1                    (0x3006, "External Voltage 1"     , WellKnownTag.Voltage, WellKnownTag.Reading, WellKnownTag.Extra1),
    ExternalVoltage2                    (0x3007, "External Voltage 2"     , WellKnownTag.Voltage, WellKnownTag.Reading, WellKnownTag.Extra2),
    //--//
    LoadVoltage                         (0x4001, "Load Voltage"                    , WellKnownTag.Load, WellKnownTag.Voltage       , WellKnownTag.Consumption),
    LoadCurrent                         (0x4002, "Load Current"                    , WellKnownTag.Load, WellKnownTag.Current       , WellKnownTag.Consumption),
    LoadPower                           (0x4003, "Load Power"                      , WellKnownTag.Load, WellKnownTag.Power         , WellKnownTag.Consumption),
    LoadVoltAmpere                      (0x4004, "Load Volt-Ampere"                , WellKnownTag.Load, WellKnownTag.VoltAmpere    , WellKnownTag.Consumption),
    LoadPowerReactive                   (0x4005, "Load Power Reactive"             , WellKnownTag.Load, WellKnownTag.PowerReactive , WellKnownTag.Consumption),
    LoadPowerFactor                     (0x4006, "Load Power Factor"               , WellKnownTag.Load, WellKnownTag.PowerFactor   , WellKnownTag.Consumption),
    LoadEnergy                          (0x4007, "Load Energy Consumption"         , WellKnownTag.Load, WellKnownTag.Energy        , WellKnownTag.Consumption),
    LoadEnergyReactive                  (0x4008, "Load Reactive Energy Consumption", WellKnownTag.Load, WellKnownTag.EnergyReactive, WellKnownTag.Consumption),
    //--//
    ChargingStatus                      (0x5001, "Charging Status"        , WellKnownTag.ChargeController, WellKnownTag.ChargingStatus                          ),
    TotalCharge                         (0x5002, "Total Charge"           , WellKnownTag.ChargeController, WellKnownTag.Charge        , WellKnownTag.Total      ),
    TotalDischarge                      (0x5003, "Total Discharge"        , WellKnownTag.ChargeController, WellKnownTag.Discharge     , WellKnownTag.Total      ),
    HeatsinkTemperature                 (0x5004, "Heatsink Temperature"   ,                                WellKnownTag.Heatsink      , WellKnownTag.Temperature),
    FaultCode                           (0x5005, "Fault Code"             ,                                WellKnownTag.FaultCode     , WellKnownTag.General    ),
    FaultCodeCharging                   (0x5006, "Charging Fault Code"    , WellKnownTag.ChargeController, WellKnownTag.FaultCode     , WellKnownTag.Charging   ),
    FaultCodeDischarging                (0x5007, "Discharging Fault Code" , WellKnownTag.ChargeController, WellKnownTag.FaultCode     , WellKnownTag.Discharging),
    //--//
    CounterResettable                   (0x6001, "Cycle Counter Resettable"    , WellKnownTag.Counter, WellKnownTag.Cycle, WellKnownTag.Resettable   ),
    CounterNonResettable                (0x6002, "Cycle Counter Non-Resettable", WellKnownTag.Counter, WellKnownTag.Cycle, WellKnownTag.NonResettable),
    //--//
    CommandOpen                         (0x7001, "Command - Open"     , WellKnownTag.Command, WellKnownTag.Open    ),
    CommandClose                        (0x7002, "Command - Close"    , WellKnownTag.Command, WellKnownTag.Close   ),
    CommandLift                         (0x7003, "Command - Lift"     , WellKnownTag.Command, WellKnownTag.Lift    ),
    CommandLower                        (0x7004, "Command - Lower"    , WellKnownTag.Command, WellKnownTag.Lower   ),
    CommandTiltUp                       (0x7005, "Command - Tilt Up"  , WellKnownTag.Command, WellKnownTag.TiltUp  ),
    CommandTiltDown                     (0x7006, "Command - Tilt Down", WellKnownTag.Command, WellKnownTag.TiltDown),
    CommandSlideIn                      (0x7007, "Command - Slide In" , WellKnownTag.Command, WellKnownTag.SlideIn ),
    CommandSlideOut                     (0x7008, "Command - Slide Out", WellKnownTag.Command, WellKnownTag.SlideOut),
    DigitalOutput                       (0x7009, "Digital Output"     , WellKnownTag.Command                       ),
    DigitalInput                        (0x700A, "Digital Input"      , WellKnownTag.Signal                        ),
    MotorSolenoid                       (0x700B, "Motor Solenoid"     , WellKnownTag.Command, WellKnownTag.Solenoid),
    //--//
    HvacTemperature                     (0x8001, "HVAC - Temperature"     , WellKnownTag.Hvac, WellKnownTag.Temperature, WellKnownTag.Reading                       ),
    HvacSetTemperature                  (0x8002, "HVAC - Set Temperature" , WellKnownTag.Hvac, WellKnownTag.Temperature, WellKnownTag.SetPoint                      ),
    HvacCompressorSpeed                 (0x8003, "HVAC - Compressor Speed", WellKnownTag.Hvac, WellKnownTag.Compressor , WellKnownTag.Velocity, WellKnownTag.Reading),
    HvacOperatingMode                   (0x8004, "HVAC - Operating Mode"  , WellKnownTag.Hvac, WellKnownTag.OperatingMode                                           ),
    HvacStateOfCharge                   (0x8005, "HVAC - State Of Charge" , WellKnownTag.Hvac, WellKnownTag.StateOfCharge                                           ),
    HvacStateOfHealth                   (0x8006, "HVAC - State Of Health" , WellKnownTag.Hvac, WellKnownTag.StateOfHealth                                           ),
    //--//
    NoIdleState                         (0x9001, "NoIdle - State"                      , WellKnownTag.NoIdle, WellKnownTag.State                                                            ),
    NoIdleSupplyVoltage                 (0x9002, "NoIdle - Supply Voltage"             , WellKnownTag.NoIdle, WellKnownTag.Voltage    , WellKnownTag.Supply                                 ),
    NoIdleOemVoltage                    (0x9003, "NoIdle - OEM Voltage"                , WellKnownTag.NoIdle, WellKnownTag.Voltage    , WellKnownTag.Oem                                    ),
    NoIdleParkNeutralVoltage            (0x9004, "NoIdle - Park/Neutral Voltage"       , WellKnownTag.NoIdle, WellKnownTag.Voltage    , WellKnownTag.ParkNeutral                            ),
    NoIdleParkingBrakeVoltage           (0x9005, "NoIdle - Parking Brake Voltage"      , WellKnownTag.NoIdle, WellKnownTag.Voltage    , WellKnownTag.ParkingBrake                           ),
    NoIdleShorelineDetectionVoltage     (0x9006, "NoIdle - Shoreline Detection Voltage", WellKnownTag.NoIdle, WellKnownTag.Voltage    , WellKnownTag.Shoreline      , WellKnownTag.Detection),
    NoIdleEmergencyLightsVoltage        (0x9007, "NoIdle - Emergency Lights Voltage"   , WellKnownTag.NoIdle, WellKnownTag.Voltage    , WellKnownTag.EmergencyLights                        ),
    NoIdleDischargeCurrent              (0x9008, "NoIdle - Battery Discharge Current"  , WellKnownTag.NoIdle, WellKnownTag.Current    , WellKnownTag.Battery        , WellKnownTag.Discharge),
    NoIdleAlternatorCurrent             (0x9009, "NoIdle - Alternator Current"         , WellKnownTag.NoIdle, WellKnownTag.Current    , WellKnownTag.Alternator                             ),
    NoIdleRelays                        (0x900A, "NoIdle - Active Relays"              , WellKnownTag.NoIdle, WellKnownTag.Relays                                                           ),
    NoIdleIgnitionSignal                (0x900B, "NoIdle - Ignition Signal"            , WellKnownTag.NoIdle, WellKnownTag.Signal     , WellKnownTag.Ignition                               ),
    NoIdleParkSignal                    (0x900C, "NoIdle - Park/Neutral Signal"        , WellKnownTag.NoIdle, WellKnownTag.Signal     , WellKnownTag.ParkNeutral                            ),
    NoIdleParkingBrakeSignal            (0x900D, "NoIdle - Parking Brake Signal"       , WellKnownTag.NoIdle, WellKnownTag.Signal     , WellKnownTag.ParkingBrake                           ),
    NoIdleHoodClosedSignal              (0x900E, "NoIdle - Hood Closed Signal"         , WellKnownTag.NoIdle, WellKnownTag.Signal     , WellKnownTag.Hood           , WellKnownTag.Closed   ),
    NoIdleEmergencyLightsSignal         (0x900F, "NoIdle - Emergency Lights Signal"    , WellKnownTag.NoIdle, WellKnownTag.Signal     , WellKnownTag.EmergencyLights                        ),
    NoIdleTemperature                   (0x9010, "NoIdle - Current Temperature"        , WellKnownTag.NoIdle, WellKnownTag.Temperature, WellKnownTag.Reading                                ),
    NoIdleMinTemperature                (0x9011, "NoIdle - Minimum Temperature Set"    , WellKnownTag.NoIdle, WellKnownTag.Temperature, WellKnownTag.SetPoint       , WellKnownTag.Minimum  ),
    NoIdleMaxTemperature                (0x9012, "NoIdle - Maximum Temperature Set"    , WellKnownTag.NoIdle, WellKnownTag.Temperature, WellKnownTag.SetPoint       , WellKnownTag.Maximum  ),
    NoIdleKeyInserted                   (0x9014, "NoIdle - Ignition Key Inserted"      , WellKnownTag.NoIdle, WellKnownTag.Status     , WellKnownTag.IgnitionKey    , WellKnownTag.Inserted ),
    NoIdleEngineRunning                 (0x9015, "NoIdle - Engine Running"             , WellKnownTag.NoIdle, WellKnownTag.Status     , WellKnownTag.Engine         , WellKnownTag.Running  ),
    NoIdleMaxDischargeTime              (0x9016, "NoIdle - Maximum Discharge Time"     , WellKnownTag.NoIdle, WellKnownTag.Time       , WellKnownTag.Discharge      , WellKnownTag.Maximum  ),
    NoIdleCutoffVoltage                 (0x9017, "NoIdle - Cutoff voltage"             , WellKnownTag.NoIdle, WellKnownTag.Voltage    , WellKnownTag.SetPoint       , WellKnownTag.Cutoff   ),
    NoIdleEngineStartCounter            (0x9018, "NoIdle - Engine Start Counter"       , WellKnownTag.NoIdle, WellKnownTag.Counter    , WellKnownTag.Engine         , WellKnownTag.Start    ),
    NoIdleEngineStopCounter             (0x9019, "NoIdle - Engine Stop Counter"        , WellKnownTag.NoIdle, WellKnownTag.Counter    , WellKnownTag.Engine         , WellKnownTag.Stop     ),
    NoIdleEmergencyLight                (0x901A, "NoIdle - Emergency Light"            , WellKnownTag.NoIdle, WellKnownTag.Command    , WellKnownTag.EmergencyLights                        ),
    NoIdleChargeEnable                  (0x901B, "NoIdle - Charge Enable"              , WellKnownTag.NoIdle, WellKnownTag.Command    , WellKnownTag.Battery, WellKnownTag.Charge           ),
    NoIdleDischargeEnable               (0x901C, "NoIdle - Discharge Enable"           , WellKnownTag.NoIdle, WellKnownTag.Command    , WellKnownTag.Battery, WellKnownTag.Discharge        ),
    NoIdleRampDoorOpen                  (0x901D, "NoIdle - Ramp Door Open"             , WellKnownTag.NoIdle, WellKnownTag.Status     , WellKnownTag.Door           , WellKnownTag.Open     ),
    NoIdleACRequest                     (0x901E, "NoIdle - AC Request"                 , WellKnownTag.NoIdle, WellKnownTag.Status     , WellKnownTag.ACRequest                              ),
    //--//
    ObdiiFaultCodes                     (0xA001, "OBD-II - Fault Codes"                  , WellKnownTag.OBDII, WellKnownTag.FaultCode),
    ObdiiTimeRunWithMalfunction         (0xA002, "OBD-II - Time run with MIL on"         , WellKnownTag.OBDII, WellKnownTag.Time       , WellKnownTag.Malfunction                    ),
    ObdiiDistanceTraveledWithMalfunction(0xA003, "OBD-II - Distance traveled with MIL on", WellKnownTag.OBDII, WellKnownTag.Distance   , WellKnownTag.Malfunction                    ),
    ObdiiEngineRPM                      (0xA004, "OBD-II - Engine Rpm"                   , WellKnownTag.OBDII, WellKnownTag.Velocity   , WellKnownTag.Engine                         ),
    ObdiiCalculatedEngineLoad           (0xA005, "OBD-II - Calculated Engine Load"       , WellKnownTag.OBDII, WellKnownTag.Load       , WellKnownTag.Engine, WellKnownTag.Calculated),
    ObdiiEngineCoolantTemperature       (0xA006, "OBD-II - Engine Coolant Temperature"   , WellKnownTag.OBDII, WellKnownTag.Temperature, WellKnownTag.EngineCoolant                  ),
    ObdiiEngineOilTemperature           (0xA007, "OBD-II - Engine Oil Temperature"       , WellKnownTag.OBDII, WellKnownTag.Temperature, WellKnownTag.EngineOil                      ),
    ObdiiVehicleSpeed                   (0xA008, "OBD-II - Vehicle Speed"                , WellKnownTag.OBDII, WellKnownTag.Velocity   , WellKnownTag.Vehicle                        ),
    ObdiiVin                            (0xA009, "OBD-II - VIN"                          , WellKnownTag.OBDII, WellKnownTag.VIN                                                      ),
    ObdiiSupportedPIDs                  (0xA00A, "OBD-II - Supported PIDs"               , WellKnownTag.OBDII, WellKnownTag.PIDS                                                     ),
    ObdiiOdometer                       (0xA00B, "OBD-II - Odometer"                     , WellKnownTag.OBDII, WellKnownTag.Distance, WellKnownTag.Odometer                          ),
    ObdiiEngineRuntime                  (0xA00C, "OBD-II - Engine Runtime"               , WellKnownTag.OBDII, WellKnownTag.Time, WellKnownTag.Engine, WellKnownTag.Present          ),
    ObdiiEngineRuntimeTotal             (0xA00D, "OBD-II - Engine Runtime Total"         , WellKnownTag.OBDII, WellKnownTag.Time, WellKnownTag.Engine, WellKnownTag.Total            ),
    //--//
    SensorTemperature                   (0xB001, "Temperature"        , WellKnownTag.Sensor, WellKnownTag.Temperature, WellKnownTag.Reading, WellKnownTag.Main  ),
    SensorPressure                      (0xB002, "Pressure"           , WellKnownTag.Sensor, WellKnownTag.Pressure   , WellKnownTag.Reading, WellKnownTag.Main  ),
    SensorRSSI                          (0xB003, "RSSI"               , WellKnownTag.Sensor, WellKnownTag.RSSI                                                  ),
    SensorSignalQuality                 (0xB004, "Signal Quality"     , WellKnownTag.Sensor, WellKnownTag.SignalQuality                                         ),
    SensorBitErrorRate                  (0xB005, "Bit Error Rate"     , WellKnownTag.Sensor, WellKnownTag.BitErrorRate                                          ),
    SensorEvent                         (0xB006, "Event"              , WellKnownTag.Sensor, WellKnownTag.Event                                                 ),
    SensorExtraTemperature1             (0xB007, "Extra Temperature 1", WellKnownTag.Sensor, WellKnownTag.Temperature, WellKnownTag.Reading, WellKnownTag.Extra1),
    SensorExtraTemperature2             (0xB008, "Extra Temperature 2", WellKnownTag.Sensor, WellKnownTag.Temperature, WellKnownTag.Reading, WellKnownTag.Extra2),
    SensorFlood                         (0xB009, "Flood"              , WellKnownTag.Sensor, WellKnownTag.Level                                                 ),
    SensorAxisX                         (0xB00A, "Axis X"             , WellKnownTag.Sensor, WellKnownTag.Acceleration, WellKnownTag.AxisX                      ),
    SensorAxisY                         (0xB00B, "Axis Y"             , WellKnownTag.Sensor, WellKnownTag.Acceleration, WellKnownTag.AxisY                      ),
    SensorAxisZ                         (0xB00C, "Axis Z"             , WellKnownTag.Sensor, WellKnownTag.Acceleration, WellKnownTag.AxisZ                      ),
    SensorAxisPitch                     (0xB00D, "Axis Pitch"         , WellKnownTag.Sensor, WellKnownTag.Orientation , WellKnownTag.AxisPitch                  ),
    SensorAxisYaw                       (0xB00E, "Axis Yaw"           , WellKnownTag.Sensor, WellKnownTag.Orientation , WellKnownTag.AxisYaw                    ),
    SensorAxisRoll                      (0xB00F, "Axis Roll"          , WellKnownTag.Sensor, WellKnownTag.Orientation , WellKnownTag.AxisRoll                   ),
    SensorLevel                         (0xB010, "Level"              , WellKnownTag.Sensor, WellKnownTag.Level                                                 ),
    SensorNoise                         (0xB011, "Noise"              , WellKnownTag.Sensor, WellKnownTag.Noise                                                 ),
    SensorAcidity                       (0xB012, "Acidity"            , WellKnownTag.Sensor, WellKnownTag.Acidity                                               ),
    SensorFrequency                     (0xB013, "Frequency"          , WellKnownTag.Sensor, WellKnownTag.Frequency                                             ),
    SensorFlow                          (0xB014, "Flow"               , WellKnownTag.Sensor, WellKnownTag.Flow                                                  ),
    SensorStatus                        (0xB015, "Status"             , WellKnownTag.Sensor, WellKnownTag.Status                                                ),
    SensorHumidity                      (0xB016, "Humidity"           , WellKnownTag.Sensor, WellKnownTag.Humidity, WellKnownTag.Reading, WellKnownTag.Main     ),
    SensorVoltage                       (0xB017, "Voltage"            , WellKnownTag.Sensor, WellKnownTag.Voltage, WellKnownTag.Reading, WellKnownTag.Main      ),
    SensorCurrent                       (0xB018, "Current"            , WellKnownTag.Sensor, WellKnownTag.Current, WellKnownTag.Reading, WellKnownTag.Main      ),
    SensorParticleMonitor               (0xB019, "Particle Monitor"   , WellKnownTag.Sensor, WellKnownTag.Particle, WellKnownTag.Reading                        ),
    //
    TrackerTrips                        (0xC001, "Trips"               ),
    TrackerInTrip                       (0xC002, "In Trip"             ),
    TrackerTamperAlert                  (0xC003, "Tamper Alert"        ),
    TrackerRecoveryModeActive           (0xC004, "Recovery Mode Active"),
    //
    // 0xD000 - 0xDFFF range was retired, available for future uses.
    //
    HolykellLevel                       (0xE001, "Liquid Level"      , WellKnownTag.Holykell, WellKnownTag.Sensor, WellKnownTag.Level      ),
    HolykellTemperature                 (0xE002, "Sensor Temperature", WellKnownTag.Holykell, WellKnownTag.Sensor, WellKnownTag.Temperature),
    //
    SurvalentAnalog                     (0xF001, "Analog", WellKnownTag.Survalent, WellKnownTag.Reading),
    SurvalentStatus                     (0xF002, "Status", WellKnownTag.Survalent, WellKnownTag.Status ),
    SurvalentText                       (0xF003, "Text"  , WellKnownTag.Survalent, WellKnownTag.Event  );
    // @formatter:on

    private final int                         m_id;
    private final String                      m_description;
    private final WellKnownTag[]              m_tags;
    private final WellKnownPointClassOrCustom m_wrapped;

    WellKnownPointClass(int id,
                        String description,
                        WellKnownTag... tags)
    {
        m_id          = id;
        m_description = description;
        m_tags        = tags;
        m_wrapped     = new WellKnownPointClassOrCustom(this, 0);
    }

    public static WellKnownPointClass parse(String name)
    {
        for (WellKnownPointClass t : values())
        {
            if (StringUtils.equals(t.name(), name))
            {
                return t;
            }

            if (StringUtils.equals(Integer.toString(t.m_id), name))
            {
                return t;
            }
        }

        return null;
    }

    public static boolean isValid(WellKnownPointClass clz)
    {
        return clz != null && clz != None;
    }

    @JsonIgnore
    public WellKnownPointClassOrCustom asWrapped()
    {
        return m_wrapped;
    }

    public int getId()
    {
        return m_id;
    }

    public boolean hasTags()
    {
        return m_tags.length > 0;
    }

    public WellKnownTag[] getWellKnownTags()
    {
        return m_tags;
    }

    public List<String> getTags()
    {
        List<String> res = Lists.newArrayList();

        for (WellKnownTag tag : m_tags)
        {
            res.add(tag.name());
        }

        return res;
    }

    @Override
    public String getDisplayName()
    {
        return name();
    }

    @Override
    public String getDescription()
    {
        return m_description;
    }
}
