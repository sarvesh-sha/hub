/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.bluesky;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("Ipn:MasterSetpointsRequest")
public class BlueSky_MasterSetpointsRequest extends BaseBlueSkyObjectModel
{
    @Override
    public String extractBaseId()
    {
        return "masterSetpointsRequest";
    }

    @Override
    public boolean postDecodingValidation()
    {
        return true;
    }
}
