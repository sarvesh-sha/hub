/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.location;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.logic.location.LocationsEngine;

@JsonTypeName("GeoFenceByRadius")
public class GeoFenceByRadius extends GeoFence
{
    public LongitudeLatitude center;
    public double            radius;

    @Override
    public void validate()
    {
        LocationsEngine.validate(this);
    }
}
