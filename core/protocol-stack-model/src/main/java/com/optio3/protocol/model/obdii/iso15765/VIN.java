/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.iso15765;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.WellKnownPointClass;
import com.optio3.protocol.model.obdii.Iso15765MessageType;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:ISO15765:VIN")
@Iso15765MessageType(service = 9, pdu = 2, hasMultipleFrames = true)
public class VIN extends BaseIso15765ObjectModel
{
    @JsonIgnore
    @SerializationTag(number = 0)
    public byte[] valueRaw;

    @FieldModelDescription(description = "VIN", units = EngineeringUnits.constant, pointClass = WellKnownPointClass.ObdiiVin, debounceSeconds = 5)
    public String value;

    //--//

    @Override
    public String extractBaseId()
    {
        return "ISO15765_VIN";
    }

    @Override
    public void postDecodeFixup()
    {
        if (valueRaw != null)
        {
            StringBuilder sb = new StringBuilder();
            for (byte b : valueRaw)
            {
                char c = (char) b;
                if (Character.isLetterOrDigit(c))
                {
                    sb.append(c);
                }
            }

            value = sb.length() > 0 ? sb.toString() : null;
        }
    }
}
