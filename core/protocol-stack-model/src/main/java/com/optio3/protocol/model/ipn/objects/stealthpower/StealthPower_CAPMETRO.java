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

@JsonTypeName("Ipn:StealthPower::CAPMETRO")
public class StealthPower_CAPMETRO extends BaseStealthPowerModel
{
    // @formatter:off
    @FieldModelDescription(description = "System State", units = EngineeringUnits.enumerated, pointClass = WellKnownPointClass.NoIdleState, indexed = true)
    public StealthPowerSystemStateForCAPMETRO system_state;

    @FieldModelDescription(description = "Supply Voltage", units = EngineeringUnits.volts, pointClass = WellKnownPointClass.NoIdleSupplyVoltage, debounceSeconds = 10, minimumDelta = 0.1)
    public float supply_voltage;

    @FieldModelDescription(description = "Battery Discharge Current", units = EngineeringUnits.amperes, pointClass = WellKnownPointClass.NoIdleDischargeCurrent, debounceSeconds = 10, minimumDelta = 5)
    public float battery_discharge_current;

    @FieldModelDescription(description = "Current Temperature", units = EngineeringUnits.degrees_celsius, pointClass = WellKnownPointClass.NoIdleTemperature, debounceSeconds = 10, minimumDelta = 0.5)
    public float temperature;

    @FieldModelDescription(description = "Engine Stop Counter", units = EngineeringUnits.counts, pointClass = WellKnownPointClass.NoIdleEngineStopCounter)
    public int engine_stop_counter;

    @FieldModelDescription(description = "Engine Start Counter", units = EngineeringUnits.counts, pointClass = WellKnownPointClass.NoIdleEngineStartCounter)
    public int engine_start_counter;

    @FieldModelDescription(description = "Ignition Key Inserted", units = EngineeringUnits.onOff, pointClass = WellKnownPointClass.NoIdleKeyInserted, debounceSeconds = 10)
    public boolean key_inserted;

    @FieldModelDescription(description = "Ignition Signal", units = EngineeringUnits.onOff, pointClass = WellKnownPointClass.NoIdleIgnitionSignal, debounceSeconds = 15)
    public boolean ignition_signal;

    @FieldModelDescription(description = "Hood Closed Signal", units = EngineeringUnits.onOff, pointClass = WellKnownPointClass.NoIdleHoodClosedSignal, debounceSeconds = 5)
    public boolean hood_closed_signal;

    @FieldModelDescription(description = "Park Signal", units = EngineeringUnits.onOff, pointClass = WellKnownPointClass.NoIdleParkSignal, debounceSeconds = 15)
    public boolean park_signal;

    @FieldModelDescription(description = "Engine Running", units = EngineeringUnits.onOff, pointClass = WellKnownPointClass.NoIdleEngineRunning, debounceSeconds = 15)
    public boolean engine_running;

    //--//

    @FieldModelDescription(description = "Ramp Door Open", units = EngineeringUnits.onOff, pointClass = WellKnownPointClass.NoIdleRampDoorOpen, debounceSeconds = 15)
    public boolean ramp_door_open;

    @FieldModelDescription(description = "AC Request", units = EngineeringUnits.onOff, pointClass = WellKnownPointClass.NoIdleACRequest, debounceSeconds = 15)
    public boolean ac_request;

    //--//

    @JsonIgnore
    public boolean reboot_flag;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "StealthPower_CAPMETRO";
    }
}
