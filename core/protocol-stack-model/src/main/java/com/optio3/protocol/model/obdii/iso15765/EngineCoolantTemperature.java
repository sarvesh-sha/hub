/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.iso15765;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.WellKnownPointClass;
import com.optio3.protocol.model.obdii.Iso15765MessageType;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:ISO15765:EngineCoolantTemperature")
@Iso15765MessageType(service = 1, pdu = 5)
public class EngineCoolantTemperature extends BaseIso15765ObjectModel
{
    @FieldModelDescription(description = "Engine Coolant Temperature", units = EngineeringUnits.degrees_celsius, pointClass = WellKnownPointClass.ObdiiEngineCoolantTemperature, debounceSeconds = 5,
                           minimumDelta = 2)
    @SerializationTag(number = 0, width = 8, scaling = { @SerializationScaling(postScalingOffset = -40, assumeUnsigned = true) })
    public float value;

    //--//

    @Override
    public String extractBaseId()
    {
        return "ISO15765_EngineCoolantTemperature";
    }
}
