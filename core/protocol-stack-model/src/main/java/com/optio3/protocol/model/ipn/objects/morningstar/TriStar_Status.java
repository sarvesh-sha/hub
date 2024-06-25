/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.morningstar;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.WellKnownPointClass;

@JsonTypeName("Ipn:Modbus:TriStar_Status")
public class TriStar_Status extends BaseTriStarModel
{
    @FieldModelDescription(description = "Battery Voltage, Filtered (1 minute)", units = EngineeringUnits.volts, debounceSeconds = 5, minimumDelta = 0.050)
    @TriStarField(pdu = 0x0026, length = 1, signed = true, fixedScaling = 1.0f / 32768, voltageScaling = true)
    public float adc_vb_f_1m;

    @FieldModelDescription(description = "Charging Current, Filtered (1 minute)", units = EngineeringUnits.amperes, debounceSeconds = 5, minimumDelta = 0.050)
    @TriStarField(pdu = 0x0027, length = 1, signed = true, fixedScaling = 1.0f / 32768, currentScaling = true)
    public float adc_ib_f_1m;

    @FieldModelDescription(description = "Minimum Battery Voltage", units = EngineeringUnits.volts, debounceSeconds = 5, minimumDelta = 0.050)
    @TriStarField(pdu = 0x0028, length = 1, signed = true, fixedScaling = 1.0f / 32768, voltageScaling = true)
    public float vb_min;

    @FieldModelDescription(description = "Maximum Battery Voltage", units = EngineeringUnits.volts, debounceSeconds = 5, minimumDelta = 0.050)
    @TriStarField(pdu = 0x0029, length = 1, signed = true, fixedScaling = 1.0f / 32768, voltageScaling = true)
    public float vb_max;

    @FieldModelDescription(description = "Hours Of Operation", units = EngineeringUnits.hours)
    @TriStarField(pdu = 0x002A, length = 2, signed = false)
    public int hourmeter;

    //--//

    @FieldModelDescription(description = "Faults", units = EngineeringUnits.enumerated, pointClass = WellKnownPointClass.FaultCode, debounceSeconds = 15)
    @TriStarField(pdu = 0x002C, length = 1, signed = false)
    public TriStarFault fault;

    //--//

    @FieldModelDescription(description = "Alarms", units = EngineeringUnits.enumerated, pointClass = WellKnownPointClass.FaultCodeCharging, debounceSeconds = 15)
    @TriStarField(pdu = 0x002E, length = 2, signed = false)
    public TriStarAlarm alarm;

    //--//

    @FieldModelDescription(description = "Dip Switches", units = EngineeringUnits.no_units)
    @TriStarField(pdu = 0x30, length = 1, signed = false)
    public int dip;

    //--//

    @FieldModelDescription(description = "LED State", units = EngineeringUnits.enumerated)
    @TriStarField(pdu = 0x31, length = 1, signed = false)
    public TriStarLed led_state;

    @TriStarField(pdu = 0x0032, length = 1, signed = false)
    @JsonIgnore
    public int reserved_0x0032; // Padding for read requests, otherwise TriStar hangs.

    //--//

    @Override
    public String extractBaseId()
    {
        return "TriStar_Status";
    }
}
