/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * Optio3 Hub APIs
 * APIs and Definitions for the Optio3 Hub product.
 *
 * OpenAPI spec version: 1.0.0
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.optio3.cloud.client.hub.model;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum WellKnownPointClass
{
    None(String.valueOf("None")),
    Log(String.valueOf("Log")),
    Ignored(String.valueOf("Ignored")),
    LocationLongitude(String.valueOf("LocationLongitude")),
    LocationLatitude(String.valueOf("LocationLatitude")),
    LocationSpeed(String.valueOf("LocationSpeed")),
    LocationAltitude(String.valueOf("LocationAltitude")),
    LocationHeading(String.valueOf("LocationHeading")),
    AccelerationX(String.valueOf("AccelerationX")),
    AccelerationY(String.valueOf("AccelerationY")),
    AccelerationZ(String.valueOf("AccelerationZ")),
    Acceleration(String.valueOf("Acceleration")),
    VelocityX(String.valueOf("VelocityX")),
    VelocityY(String.valueOf("VelocityY")),
    VelocityZ(String.valueOf("VelocityZ")),
    Velocity(String.valueOf("Velocity")),
    ArrayVoltage(String.valueOf("ArrayVoltage")),
    ArrayCurrent(String.valueOf("ArrayCurrent")),
    ArrayPower(String.valueOf("ArrayPower")),
    BatteryVoltage(String.valueOf("BatteryVoltage")),
    BatteryCurrent(String.valueOf("BatteryCurrent")),
    BatteryPower(String.valueOf("BatteryPower")),
    BatteryStateOfCharge(String.valueOf("BatteryStateOfCharge")),
    BatteryTemperature(String.valueOf("BatteryTemperature")),
    ExternalVoltage1(String.valueOf("ExternalVoltage1")),
    ExternalVoltage2(String.valueOf("ExternalVoltage2")),
    LoadVoltage(String.valueOf("LoadVoltage")),
    LoadCurrent(String.valueOf("LoadCurrent")),
    LoadPower(String.valueOf("LoadPower")),
    LoadVoltAmpere(String.valueOf("LoadVoltAmpere")),
    LoadPowerReactive(String.valueOf("LoadPowerReactive")),
    LoadPowerFactor(String.valueOf("LoadPowerFactor")),
    LoadEnergy(String.valueOf("LoadEnergy")),
    LoadEnergyReactive(String.valueOf("LoadEnergyReactive")),
    ChargingStatus(String.valueOf("ChargingStatus")),
    TotalCharge(String.valueOf("TotalCharge")),
    TotalDischarge(String.valueOf("TotalDischarge")),
    HeatsinkTemperature(String.valueOf("HeatsinkTemperature")),
    FaultCode(String.valueOf("FaultCode")),
    FaultCodeCharging(String.valueOf("FaultCodeCharging")),
    FaultCodeDischarging(String.valueOf("FaultCodeDischarging")),
    CounterResettable(String.valueOf("CounterResettable")),
    CounterNonResettable(String.valueOf("CounterNonResettable")),
    CommandOpen(String.valueOf("CommandOpen")),
    CommandClose(String.valueOf("CommandClose")),
    CommandLift(String.valueOf("CommandLift")),
    CommandLower(String.valueOf("CommandLower")),
    CommandTiltUp(String.valueOf("CommandTiltUp")),
    CommandTiltDown(String.valueOf("CommandTiltDown")),
    CommandSlideIn(String.valueOf("CommandSlideIn")),
    CommandSlideOut(String.valueOf("CommandSlideOut")),
    DigitalOutput(String.valueOf("DigitalOutput")),
    DigitalInput(String.valueOf("DigitalInput")),
    MotorSolenoid(String.valueOf("MotorSolenoid")),
    HvacTemperature(String.valueOf("HvacTemperature")),
    HvacSetTemperature(String.valueOf("HvacSetTemperature")),
    HvacCompressorSpeed(String.valueOf("HvacCompressorSpeed")),
    HvacOperatingMode(String.valueOf("HvacOperatingMode")),
    HvacStateOfCharge(String.valueOf("HvacStateOfCharge")),
    HvacStateOfHealth(String.valueOf("HvacStateOfHealth")),
    NoIdleState(String.valueOf("NoIdleState")),
    NoIdleSupplyVoltage(String.valueOf("NoIdleSupplyVoltage")),
    NoIdleOemVoltage(String.valueOf("NoIdleOemVoltage")),
    NoIdleParkNeutralVoltage(String.valueOf("NoIdleParkNeutralVoltage")),
    NoIdleParkingBrakeVoltage(String.valueOf("NoIdleParkingBrakeVoltage")),
    NoIdleShorelineDetectionVoltage(String.valueOf("NoIdleShorelineDetectionVoltage")),
    NoIdleEmergencyLightsVoltage(String.valueOf("NoIdleEmergencyLightsVoltage")),
    NoIdleDischargeCurrent(String.valueOf("NoIdleDischargeCurrent")),
    NoIdleAlternatorCurrent(String.valueOf("NoIdleAlternatorCurrent")),
    NoIdleRelays(String.valueOf("NoIdleRelays")),
    NoIdleIgnitionSignal(String.valueOf("NoIdleIgnitionSignal")),
    NoIdleParkSignal(String.valueOf("NoIdleParkSignal")),
    NoIdleParkingBrakeSignal(String.valueOf("NoIdleParkingBrakeSignal")),
    NoIdleHoodClosedSignal(String.valueOf("NoIdleHoodClosedSignal")),
    NoIdleEmergencyLightsSignal(String.valueOf("NoIdleEmergencyLightsSignal")),
    NoIdleTemperature(String.valueOf("NoIdleTemperature")),
    NoIdleMinTemperature(String.valueOf("NoIdleMinTemperature")),
    NoIdleMaxTemperature(String.valueOf("NoIdleMaxTemperature")),
    NoIdleKeyInserted(String.valueOf("NoIdleKeyInserted")),
    NoIdleEngineRunning(String.valueOf("NoIdleEngineRunning")),
    NoIdleMaxDischargeTime(String.valueOf("NoIdleMaxDischargeTime")),
    NoIdleCutoffVoltage(String.valueOf("NoIdleCutoffVoltage")),
    NoIdleEngineStartCounter(String.valueOf("NoIdleEngineStartCounter")),
    NoIdleEngineStopCounter(String.valueOf("NoIdleEngineStopCounter")),
    NoIdleEmergencyLight(String.valueOf("NoIdleEmergencyLight")),
    NoIdleChargeEnable(String.valueOf("NoIdleChargeEnable")),
    NoIdleDischargeEnable(String.valueOf("NoIdleDischargeEnable")),
    NoIdleRampDoorOpen(String.valueOf("NoIdleRampDoorOpen")),
    NoIdleACRequest(String.valueOf("NoIdleACRequest")),
    ObdiiFaultCodes(String.valueOf("ObdiiFaultCodes")),
    ObdiiTimeRunWithMalfunction(String.valueOf("ObdiiTimeRunWithMalfunction")),
    ObdiiDistanceTraveledWithMalfunction(String.valueOf("ObdiiDistanceTraveledWithMalfunction")),
    ObdiiEngineRPM(String.valueOf("ObdiiEngineRPM")),
    ObdiiCalculatedEngineLoad(String.valueOf("ObdiiCalculatedEngineLoad")),
    ObdiiEngineCoolantTemperature(String.valueOf("ObdiiEngineCoolantTemperature")),
    ObdiiEngineOilTemperature(String.valueOf("ObdiiEngineOilTemperature")),
    ObdiiVehicleSpeed(String.valueOf("ObdiiVehicleSpeed")),
    ObdiiVin(String.valueOf("ObdiiVin")),
    ObdiiSupportedPIDs(String.valueOf("ObdiiSupportedPIDs")),
    ObdiiOdometer(String.valueOf("ObdiiOdometer")),
    ObdiiEngineRuntime(String.valueOf("ObdiiEngineRuntime")),
    ObdiiEngineRuntimeTotal(String.valueOf("ObdiiEngineRuntimeTotal")),
    SensorTemperature(String.valueOf("SensorTemperature")),
    SensorPressure(String.valueOf("SensorPressure")),
    SensorRSSI(String.valueOf("SensorRSSI")),
    SensorSignalQuality(String.valueOf("SensorSignalQuality")),
    SensorBitErrorRate(String.valueOf("SensorBitErrorRate")),
    SensorEvent(String.valueOf("SensorEvent")),
    SensorExtraTemperature1(String.valueOf("SensorExtraTemperature1")),
    SensorExtraTemperature2(String.valueOf("SensorExtraTemperature2")),
    SensorFlood(String.valueOf("SensorFlood")),
    SensorAxisX(String.valueOf("SensorAxisX")),
    SensorAxisY(String.valueOf("SensorAxisY")),
    SensorAxisZ(String.valueOf("SensorAxisZ")),
    SensorAxisPitch(String.valueOf("SensorAxisPitch")),
    SensorAxisYaw(String.valueOf("SensorAxisYaw")),
    SensorAxisRoll(String.valueOf("SensorAxisRoll")),
    SensorLevel(String.valueOf("SensorLevel")),
    SensorNoise(String.valueOf("SensorNoise")),
    SensorAcidity(String.valueOf("SensorAcidity")),
    SensorFrequency(String.valueOf("SensorFrequency")),
    SensorFlow(String.valueOf("SensorFlow")),
    SensorStatus(String.valueOf("SensorStatus")),
    SensorHumidity(String.valueOf("SensorHumidity")),
    SensorVoltage(String.valueOf("SensorVoltage")),
    SensorCurrent(String.valueOf("SensorCurrent")),
    SensorParticleMonitor(String.valueOf("SensorParticleMonitor")),
    TrackerTrips(String.valueOf("TrackerTrips")),
    TrackerInTrip(String.valueOf("TrackerInTrip")),
    TrackerTamperAlert(String.valueOf("TrackerTamperAlert")),
    TrackerRecoveryModeActive(String.valueOf("TrackerRecoveryModeActive")),
    HolykellLevel(String.valueOf("HolykellLevel")),
    HolykellTemperature(String.valueOf("HolykellTemperature")),
    SurvalentAnalog(String.valueOf("SurvalentAnalog")),
    SurvalentStatus(String.valueOf("SurvalentStatus")),
    SurvalentText(String.valueOf("SurvalentText"));

    private String value;

    WellKnownPointClass(String v)
    {
        value = v;
    }

    public String value()
    {
        return value;
    }

    @Override
    public String toString()
    {
        return String.valueOf(value);
    }

    @JsonCreator
    public static WellKnownPointClass fromValue(String v)
    {
        for (WellKnownPointClass b : WellKnownPointClass.values())
        {
            if (String.valueOf(b.value).equals(v))
                return b;
        }

        throw new IllegalArgumentException(String.format("'%s' is not one of the values accepted for %s class: %s", v, WellKnownPointClass.class.getSimpleName(), Arrays.toString(WellKnownPointClass.values())));
    }
}
