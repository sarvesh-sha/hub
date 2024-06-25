/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.morningstar;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;

@JsonTypeName("Ipn:Modbus:TriStar_MPPT")
public class TriStar_MPPT extends BaseTriStarModel
{
    @FieldModelDescription(description = "Output power", units = EngineeringUnits.watts, debounceSeconds = 5, minimumDelta = 0.5)
    @TriStarField(pdu = 0x003A, length = 1, signed = false, fixedScaling = 1.0f / (1 << 17), voltageScaling = true, currentScaling = true)
    public float power_out_shadow;

    @FieldModelDescription(description = "Input power", units = EngineeringUnits.watts, debounceSeconds = 5, minimumDelta = 0.5)
    @TriStarField(pdu = 0x003B, length = 1, signed = false, fixedScaling = 1.0f / (1 << 17), voltageScaling = true, currentScaling = true)
    public float power_in_shadow;

    @FieldModelDescription(description = "Maximum Power of last sweep", units = EngineeringUnits.watts, debounceSeconds = 5, minimumDelta = 0.5)
    @TriStarField(pdu = 0x003C, length = 1, signed = false, fixedScaling = 1.0f / (1 << 17), voltageScaling = true, currentScaling = true)
    public float sweep_Pin_max;

    @FieldModelDescription(description = "Vmp of last sweep", units = EngineeringUnits.volts, debounceSeconds = 5, minimumDelta = 0.050)
    @TriStarField(pdu = 0x003D, length = 1, signed = true, fixedScaling = 1.0f / 32768, voltageScaling = true)
    public float sweep_vmp;

    @FieldModelDescription(description = "Voc of last sweep", units = EngineeringUnits.volts, debounceSeconds = 5, minimumDelta = 0.050)
    @TriStarField(pdu = 0x003E, length = 1, signed = true, fixedScaling = 1.0f / 32768, voltageScaling = true)
    public float sweep_voc;

    //--//

    @Override
    public String extractBaseId()
    {
        return "TriStar_MPPT";
    }
}
