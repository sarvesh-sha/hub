/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.stealthpower;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.WellKnownPointClass;

@JsonTypeName("Ipn:StealthPower::FDNY")
public class StealthPower_FDNY extends BaseStealthPowerModel
{
    // @formatter:off
    @FieldModelDescription(description = "System State", units = EngineeringUnits.enumerated, pointClass = WellKnownPointClass.NoIdleState, indexed = true)
    public StealthPowerSystemState system_state;

    @FieldModelDescription(description = "Supply Voltage", units = EngineeringUnits.volts, pointClass = WellKnownPointClass.NoIdleSupplyVoltage, debounceSeconds = 10, minimumDelta = 0.1)
    public float supply_voltage;

    @FieldModelDescription(description = "OEM Voltage", units = EngineeringUnits.volts, pointClass = WellKnownPointClass.NoIdleOemVoltage, debounceSeconds = 10, minimumDelta = 0.1)
    public float oem_voltage;

    @FieldModelDescription(description = "Shoreline Detection Voltage", units = EngineeringUnits.volts, pointClass = WellKnownPointClass.NoIdleShorelineDetectionVoltage, debounceSeconds = 10, minimumDelta = 0.1)
    public float shoreline_detection_voltage;

    @FieldModelDescription(description = "Emergency Lights Voltage", units = EngineeringUnits.volts, pointClass = WellKnownPointClass.NoIdleEmergencyLightsVoltage, debounceSeconds = 10, minimumDelta = 0.1)
    public float emergency_lights_voltage;

    @FieldModelDescription(description = "Battery Discharge Current", units = EngineeringUnits.amperes, pointClass = WellKnownPointClass.NoIdleDischargeCurrent, debounceSeconds = 10, minimumDelta = 5)
    public float battery_discharge_current;

    @FieldModelDescription(description = "Active Relays", units = EngineeringUnits.counts, pointClass = WellKnownPointClass.NoIdleRelays, debounceSeconds = 5)
    public int active_relays;

    @FieldModelDescription(description = "Ignition Signal", units = EngineeringUnits.onOff, pointClass = WellKnownPointClass.NoIdleIgnitionSignal, debounceSeconds = 15)
    public boolean ignition_signal;

    @FieldModelDescription(description = "Park Signal", units = EngineeringUnits.onOff, pointClass = WellKnownPointClass.NoIdleParkSignal, debounceSeconds = 15)
    public boolean park_signal;

    @FieldModelDescription(description = "Foot Brake Signal", units = EngineeringUnits.onOff, pointClass = WellKnownPointClass.NoIdleParkingBrakeSignal, debounceSeconds = 15)
    public boolean foot_brake_signal;

    @FieldModelDescription(description = "Emergency Lights Signal", units = EngineeringUnits.onOff, pointClass = WellKnownPointClass.NoIdleEmergencyLightsSignal, debounceSeconds = 15)
    public boolean emergency_lights_signal;

    @JsonIgnore
    public boolean reboot_flag;

    @JsonIgnore
    public int activity_timer;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "StealthPower_FDNY";
    }
}
