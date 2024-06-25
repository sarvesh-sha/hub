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

@JsonTypeName("Ipn:Modbus:TriStar_Charger")
public class TriStar_Charger extends BaseTriStarModel
{
    @FieldModelDescription(description = "Charging stage", units = EngineeringUnits.enumerated, pointClass = WellKnownPointClass.ChargingStatus, debounceSeconds = 5)
    @TriStarField(pdu = 0x0032, length = 1, signed = false)
    public TriStarCharger charge_state;

    @FieldModelDescription(description = "Target regulation voltage", units = EngineeringUnits.volts, debounceSeconds = 5, minimumDelta = 0.100)
    @TriStarField(pdu = 0x0033, length = 1, signed = true, fixedScaling = 1.0f / 32768, voltageScaling = true)
    public float vb_ref;

    @FieldModelDescription(description = "Ah charge – resetable", units = EngineeringUnits.ampere_hours)
    @TriStarField(pdu = 0x0034, length = 2, signed = false, fixedScaling = 0.1f)
    public float Ahc_r;

    @FieldModelDescription(description = "Ah charge – total", units = EngineeringUnits.ampere_hours, pointClass = WellKnownPointClass.TotalCharge)
    @TriStarField(pdu = 0x0036, length = 2, signed = false, fixedScaling = 0.1f)
    public float Ahc_t;

    @FieldModelDescription(description = "kWhr charge resetable", units = EngineeringUnits.kilowatt_hours)
    @TriStarField(pdu = 0x0038, length = 1, signed = false)
    public float kwhc_r;

    @FieldModelDescription(description = "kWhr charge total", units = EngineeringUnits.kilowatt_hours)
    @TriStarField(pdu = 0x0039, length = 1, signed = false)
    public float kwhc_t;

    //--//

    @Override
    public String extractBaseId()
    {
        return "TriStar_Charger";
    }
}
