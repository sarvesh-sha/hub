/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.epsolar;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;

@JsonTypeName("Ipn:Modbus:EpSolar_LoadParameters")
public class EpSolar_LoadParameters extends BaseEpSolarModel
{
    @FieldModelDescription(description = "Night time threshold voltage", units = EngineeringUnits.volts)
    @EpSolarField(pdu = 0x901E, length = 1, signed = false, writable = true, fixedScaling = 1.0f / 100)
    public float nttv;

    @FieldModelDescription(description = "Light signal startup (night) delay time", units = EngineeringUnits.minutes)
    @EpSolarField(pdu = 0x901F, length = 1, signed = false, writable = true)
    public int light_startup_delay;

    @FieldModelDescription(description = "Day time threshold voltage", units = EngineeringUnits.volts)
    @EpSolarField(pdu = 0x9020, length = 1, signed = false, writable = true, fixedScaling = 1.0f / 100)
    public float dttv;

    @FieldModelDescription(description = "Light signal shutdown (day) delay time", units = EngineeringUnits.minutes)
    @EpSolarField(pdu = 0x9021, length = 1, signed = false, writable = true)
    public int light_shutdown_delay;

    @FieldModelDescription(description = "Load control mode", units = EngineeringUnits.enumerated)
    @EpSolarField(pdu = 0x903D, length = 1, signed = false, writable = true)
    public EpSolarLoadControlMode load_control_mode;

    //--//

    @Override
    public String extractBaseId()
    {
        return "EpSolar_LoadParameters";
    }
}
