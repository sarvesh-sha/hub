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

@JsonTypeName("Ipn:Modbus:TriStar_Temperatures")
public class TriStar_Temperatures extends BaseTriStarModel
{
    @FieldModelDescription(description = "Heatsink Temperature", units = EngineeringUnits.degrees_celsius, pointClass = WellKnownPointClass.HeatsinkTemperature, debounceSeconds = 5,
                           minimumDelta = 0.050)
    @TriStarField(pdu = 0x0023, length = 1, signed = true)
    public float T_hs;

    @FieldModelDescription(description = "RTS Temperature", units = EngineeringUnits.degrees_celsius, debounceSeconds = 5, minimumDelta = 0.050)
    @TriStarField(pdu = 0x0024, length = 1, signed = true)
    public float T_rts;

    @FieldModelDescription(description = "Battery Regulation Temperature", units = EngineeringUnits.degrees_celsius, debounceSeconds = 5, minimumDelta = 0.050)
    @TriStarField(pdu = 0x0025, length = 1, signed = true)
    public float T_batt;

    //--//

    @Override
    public String extractBaseId()
    {
        return "TriStar_Temperatures";
    }
}
