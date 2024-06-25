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

@JsonTypeName("Ipn:StealthPower::AMR")
public class StealthPower_AMR extends BaseStealthPowerModel
{
    // @formatter:off
    @FieldModelDescription(description = "System State", units = EngineeringUnits.enumerated, pointClass = WellKnownPointClass.NoIdleState, indexed = true)
    public StealthPowerSystemStateForAMR system_state;

    @FieldModelDescription(description = "Supply Voltage", units = EngineeringUnits.volts, pointClass = WellKnownPointClass.NoIdleSupplyVoltage, debounceSeconds = 10, minimumDelta = 0.1)
    public float supply_voltage;

    @FieldModelDescription(description = "OEM Voltage", units = EngineeringUnits.volts, pointClass = WellKnownPointClass.NoIdleOemVoltage, debounceSeconds = 10, minimumDelta = 0.1)
    public float oem_voltage;

    @FieldModelDescription(description = "Current Temperature", units = EngineeringUnits.degrees_celsius, pointClass = WellKnownPointClass.NoIdleTemperature, debounceSeconds = 10, minimumDelta = 0.5)
    public float temperature;

    @FieldModelDescription(description = "Engine Stop Counter", units = EngineeringUnits.counts, pointClass = WellKnownPointClass.NoIdleEngineStopCounter)
    public int engine_stop_counter;

    @FieldModelDescription(description = "Engine Start Counter", units = EngineeringUnits.counts, pointClass = WellKnownPointClass.NoIdleEngineStartCounter)
    public int engine_start_counter;

    @FieldModelDescription(description = "Hood Closed Signal", units = EngineeringUnits.onOff, pointClass = WellKnownPointClass.NoIdleHoodClosedSignal, debounceSeconds = 5)
    public boolean hood_closed_signal;

    @FieldModelDescription(description = "Park Signal", units = EngineeringUnits.onOff, pointClass = WellKnownPointClass.NoIdleParkSignal, debounceSeconds = 15)
    public boolean park_signal;

    @FieldModelDescription(description = "Ignition Key Inserted", units = EngineeringUnits.onOff, pointClass = WellKnownPointClass.NoIdleKeyInserted, debounceSeconds = 10)
    public boolean key_inserted;

    @FieldModelDescription(description = "Engine Running", units = EngineeringUnits.onOff, pointClass = WellKnownPointClass.NoIdleEngineRunning, debounceSeconds = 15)
    public boolean engine_running;

    @FieldModelDescription(description = "Emergency Light", units = EngineeringUnits.onOff, pointClass = WellKnownPointClass.NoIdleEmergencyLight, debounceSeconds = 15)
    public boolean emergency_light;

    //--//

    @FieldModelDescription(description = "Maximum Discharge Time", units = EngineeringUnits.seconds, pointClass = WellKnownPointClass.NoIdleMaxDischargeTime, debounceSeconds = 15)
    public int max_discharge_time;

    @FieldModelDescription(description = "Minimum Temperature Set", units = EngineeringUnits.degrees_celsius, pointClass = WellKnownPointClass.NoIdleMinTemperature, debounceSeconds = 10)
    public float min_temperature;

    @FieldModelDescription(description = "Maximum Temperature Set", units = EngineeringUnits.degrees_celsius, pointClass = WellKnownPointClass.NoIdleMaxTemperature, debounceSeconds = 10)
    public float max_temperature;

    @FieldModelDescription(description = "Cutoff Voltage", units = EngineeringUnits.volts, pointClass = WellKnownPointClass.NoIdleCutoffVoltage, debounceSeconds = 10, minimumDelta = 0.1)
    public float cutoff_voltage;

    //--//

    @JsonIgnore
    public boolean reboot_flag;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "StealthPower_AMR";
    }
}
