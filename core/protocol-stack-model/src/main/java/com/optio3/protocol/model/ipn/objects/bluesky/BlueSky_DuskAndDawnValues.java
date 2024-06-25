/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.bluesky;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

//
// Byte 7: post-dusk hours x10
// Byte 8: pre-dawn hours x10
//

@JsonTypeName("Ipn:DuskAndDawnValues")
public class BlueSky_DuskAndDawnValues extends BaseBlueSkyObjectModel
{
    @FieldModelDescription(description = "Post-Dusk Lights", units = EngineeringUnits.hours)
    @SerializationTag(number = 7, width = 8, scaling = { @SerializationScaling(assumeUnsigned = true, scalingFactor = 0.1) })
    public float postDusk;

    @FieldModelDescription(description = "Pre-Dawn Lights", units = EngineeringUnits.hours)
    @SerializationTag(number = 8, width = 8, scaling = { @SerializationScaling(assumeUnsigned = true, scalingFactor = 0.1) })
    public float preDawn;

    //--//

    @Override
    public String extractBaseId()
    {
        return "duskAndDawn";
    }

    @Override
    public boolean postDecodingValidation()
    {
        return true;
    }
}
