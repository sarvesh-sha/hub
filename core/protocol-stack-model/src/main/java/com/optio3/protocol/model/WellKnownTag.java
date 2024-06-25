/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model;

import org.apache.commons.lang3.StringUtils;

public enum WellKnownTag
{
    None,
    //--//
    //
    // Aggregation modifiers
    //
    Minimum,
    Average,
    Maximum,
    Total,
    Present,
    //--//
    //
    // Physical quantities
    //
    Acceleration,
    Acidity,
    Altitude,
    BitErrorRate,
    Charge,
    Current,
    Discharge,
    Distance,
    Energy,
    EnergyReactive,
    Flow,
    Frequency,
    Heading,
    Humidity,
    Latitude,
    Level,
    Longitude,
    Noise,
    Orientation,
    Particle,
    Power,
    PowerFactor,
    PowerReactive,
    Pressure,
    RSSI,
    SignalQuality,
    Temperature,
    Time,
    Velocity,
    VoltAmpere,
    Voltage,
    //--//
    AxisX,
    AxisY,
    AxisZ,
    AxisPitch,
    AxisYaw,
    AxisRoll,
    //--//
    //
    // Subjects
    //
    ACRequest,
    Alternator,
    Battery,
    ChargeController,
    Compressor,
    Counter,
    Cutoff,
    Cycle,
    Door,
    EmergencyLights,
    Engine,
    EngineCoolant,
    EngineOil,
    EngineRunning,
    Heatsink,
    Hood,
    Hvac,
    Ignition,
    IgnitionKey,
    Load,
    Location,
    Log,
    Malfunction,
    Odometer,
    Oem,
    PIDS,
    Park,
    ParkNeutral,
    ParkingBrake,
    Relays,
    Sensor,
    Shoreline,
    SolarPanel,
    Solenoid,
    Supply,
    VIN,
    Vehicle,
    //--//
    //
    // Conditions
    //
    Production,
    Consumption,
    OperatingMode,
    StateOfCharge,
    StateOfHealth,
    ChargingStatus,
    FaultCode,
    General,
    Charging,
    Discharging,
    //--//
    Resettable,
    NonResettable,
    //--//
    //
    // Purpose
    //
    Command,
    Status,
    Signal,
    State,
    Detection,
    Calculated,
    Event,

    //
    // Operations
    //
    Open,
    Close,
    Opened,
    Closed,

    Lift,
    Lower,
    TiltUp,
    TiltDown,
    SlideIn,
    SlideOut,
    Start,
    Stop,
    Inserted,
    Running,

    //
    // Selection modifiers
    //
    Reading,
    SetPoint,
    Main,
    Extra1,
    Extra2,

    //
    // Products
    //
    Digineous,
    Holykell,
    OBDII,
    NoIdle,
    Palfinger,
    Survalent;

    public static WellKnownTag parse(String name)
    {
        for (WellKnownTag t : values())
        {
            if (StringUtils.equals(t.name(), name))
            {
                return t;
            }
        }

        return null;
    }
}
