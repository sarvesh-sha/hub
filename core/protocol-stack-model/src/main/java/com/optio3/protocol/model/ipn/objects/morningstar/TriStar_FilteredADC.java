/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.morningstar;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.WellKnownPointClass;

@JsonTypeName("Ipn:Modbus:TriStar_FilteredADC")
public class TriStar_FilteredADC extends BaseTriStarModel
{
    @FieldModelDescription(description = "Battery Voltage, Filtered", units = EngineeringUnits.volts, pointClass = WellKnownPointClass.BatteryVoltage, debounceSeconds = 5, minimumDelta = 0.100)
    @TriStarField(pdu = 0x0018, length = 1, signed = true, fixedScaling = 1.0f / 32768, voltageScaling = true)
    public float adc_vb_f_med;

    @FieldModelDescription(description = "Battery Terminal Voltage, Filtered", units = EngineeringUnits.volts, debounceSeconds = 5, minimumDelta = 0.100)
    @TriStarField(pdu = 0x0019, length = 1, signed = true, fixedScaling = 1.0f / 32768, voltageScaling = true)
    public float adc_vbterm_f;

    @FieldModelDescription(description = "Battery Sense Voltage, Filtered", units = EngineeringUnits.volts, debounceSeconds = 5, minimumDelta = 0.100)
    @TriStarField(pdu = 0x001A, length = 1, signed = true, fixedScaling = 1.0f / 32768, voltageScaling = true)
    public float adc_vbs_f;

    @FieldModelDescription(description = "Array Voltage, Filtered", units = EngineeringUnits.volts, pointClass = WellKnownPointClass.ArrayVoltage, debounceSeconds = 5, minimumDelta = 0.100)
    @TriStarField(pdu = 0x001B, length = 1, signed = true, fixedScaling = 1.0f / 32768, voltageScaling = true)
    public float adc_va_f;

    @FieldModelDescription(description = "Battery Current, Filtered", units = EngineeringUnits.amperes, pointClass = WellKnownPointClass.BatteryCurrent, debounceSeconds = 5, minimumDelta = 0.050)
    @TriStarField(pdu = 0x001C, length = 1, signed = true, fixedScaling = 1.0f / 32768, currentScaling = true)
    public float adc_ib_f_shadow;

    @FieldModelDescription(description = "Array Current, Filtered", units = EngineeringUnits.amperes, pointClass = WellKnownPointClass.ArrayCurrent, debounceSeconds = 5, minimumDelta = 0.050)
    @TriStarField(pdu = 0x001D, length = 1, signed = true, fixedScaling = 1.0f / 32768, currentScaling = true)
    public float adc_ia_f_shadow;

    @FieldModelDescription(description = "12 Volt Supply, Filtered", units = EngineeringUnits.volts, debounceSeconds = 5, minimumDelta = 0.100)
    @TriStarField(pdu = 0x001E, length = 1, signed = true, fixedScaling = 18.612f / 32768)
    public float adc_p12_f;

    @FieldModelDescription(description = "3 Volt Supply, Filtered", units = EngineeringUnits.volts, debounceSeconds = 5, minimumDelta = 0.100)
    @TriStarField(pdu = 0x001F, length = 1, signed = true, fixedScaling = 6.6f / 32768)
    public float adc_p3_f;

    @FieldModelDescription(description = "MeterBus Voltage, Filtered", units = EngineeringUnits.volts, debounceSeconds = 5, minimumDelta = 0.100)
    @TriStarField(pdu = 0x0020, length = 1, signed = true, fixedScaling = 18.612f / 32768)
    public float adc_pmeter_f;

    @FieldModelDescription(description = "1.8 Volt Supply, Filtered", units = EngineeringUnits.volts, debounceSeconds = 5, minimumDelta = 0.050)
    @TriStarField(pdu = 0x0021, length = 1, signed = true, fixedScaling = 3f / 32768)
    public float adc_p18_f;

    @FieldModelDescription(description = "Reference Voltage, Filtered", units = EngineeringUnits.volts, debounceSeconds = 5, minimumDelta = 0.050)
    @TriStarField(pdu = 0x0022, length = 1, signed = true, fixedScaling = 3f / 32768)
    public float adc_v_ref;

    //--//

    @Override
    public String extractBaseId()
    {
        return "TriStar_FilteredADC";
    }
}
