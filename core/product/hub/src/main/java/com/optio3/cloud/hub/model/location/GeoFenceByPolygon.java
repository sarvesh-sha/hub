/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.location;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.logic.location.LocationsEngine;

@JsonTypeName("GeoFenceByPolygon")
public class GeoFenceByPolygon extends GeoFence
{
    public LocationPolygon       boundary;
    public List<LocationPolygon> innerExclusions;

    @Override
    public void validate()
    {
        LocationsEngine.validate(this);
    }
}
