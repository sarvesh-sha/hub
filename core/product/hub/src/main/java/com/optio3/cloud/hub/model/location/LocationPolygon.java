/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.location;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.cloud.hub.logic.location.LocationsEngine;

public class LocationPolygon
{
    public List<LongitudeLatitude> points;

    public void validate()
    {
        LocationsEngine.validate(this);
    }

    public static LocationPolygon from(LongitudeLatitude... args)
    {
        LocationPolygon res = new LocationPolygon();
        res.points = Lists.newArrayList(args);
        return res;
    }
}