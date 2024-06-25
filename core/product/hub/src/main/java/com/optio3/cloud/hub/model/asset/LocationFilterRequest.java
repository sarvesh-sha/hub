/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.asset;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("LocationFilterRequest")
public class LocationFilterRequest extends AssetFilterRequest
{
    @Override
    public boolean forceLike()
    {
        return true;
    }

    public boolean hasGeoFences;
}
