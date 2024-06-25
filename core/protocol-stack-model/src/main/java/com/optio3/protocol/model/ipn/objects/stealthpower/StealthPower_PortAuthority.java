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

@JsonTypeName("Ipn:StealthPower::PortAuthority")
public class StealthPower_PortAuthority extends BaseStealthPowerModel
{
    // @formatter:off
    @FieldModelDescription(description = "System State", units = EngineeringUnits.enumerated, pointClass = WellKnownPointClass.NoIdleState, indexed = true)
    public StealthPowerSystemState system_state;

    @FieldModelDescription(description = "Supply Voltage", units = EngineeringUnits.volts, pointClass = WellKnownPointClass.NoIdleSupplyVoltage, debounceSeconds = 5, minimumDelta = 0.1)
    public float supply_voltage;

    @FieldModelDescription(description = "OEM Voltage", units = EngineeringUnits.volts, pointClass = WellKnownPointClass.NoIdleOemVoltage, debounceSeconds = 5, minimumDelta = 0.1)
    public float oem_voltage;

    @FieldModelDescription(description = "Park/Neutral Voltage", units = EngineeringUnits.volts, pointClass = WellKnownPointClass.NoIdleParkNeutralVoltage, debounceSeconds = 5, minimumDelta = 0.1)
    public float park_neutral_voltage;

    @FieldModelDescription(description = "Parking Brake Voltage", units = EngineeringUnits.volts, pointClass = WellKnownPointClass.NoIdleParkingBrakeVoltage, debounceSeconds = 5, minimumDelta = 0.1)
    public float parking_brake_voltage;

    @FieldModelDescription(description = "Alternator Current", units = EngineeringUnits.amperes, pointClass = WellKnownPointClass.NoIdleAlternatorCurrent, debounceSeconds = 5, minimumDelta = 2)
    public float alternator_current;

    @FieldModelDescription(description = "Active Relays", units = EngineeringUnits.counts, pointClass = WellKnownPointClass.NoIdleRelays, debounceSeconds = 5)
    public int active_relays;

    @FieldModelDescription(description = "Ignition Signal", units = EngineeringUnits.onOff, pointClass = WellKnownPointClass.NoIdleIgnitionSignal, debounceSeconds = 5)
    public boolean ignition_signal;

    @FieldModelDescription(description = "Park/Neutral Signal", units = EngineeringUnits.onOff, pointClass = WellKnownPointClass.NoIdleParkSignal, debounceSeconds = 5)
    public boolean park_neutral_signal;

    @FieldModelDescription(description = "Hood Closed Signal", units = EngineeringUnits.onOff, pointClass = WellKnownPointClass.NoIdleHoodClosedSignal, debounceSeconds = 5)
    public boolean hood_closed_signal;

    @FieldModelDescription(description = "Parking Brake Signal", units = EngineeringUnits.onOff, pointClass = WellKnownPointClass.NoIdleParkingBrakeSignal, debounceSeconds = 5)
    public boolean parking_brake_signal;

    @JsonIgnore
    public boolean reboot_flag;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "StealthPower_PortAuthority";
    }
}
