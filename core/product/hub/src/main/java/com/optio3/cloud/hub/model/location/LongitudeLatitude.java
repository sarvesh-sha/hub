/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.location;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mapbox.geojson.Point;
import com.mapbox.turf.TurfMeasurement;
import com.optio3.cloud.hub.logic.location.LocationsEngine;

public class LongitudeLatitude
{
    public double longitude;
    public double latitude;

    @JsonIgnore
    public boolean isValid()
    {
        return latitude != 0.0 && longitude != 0.0;
    }

    public static LongitudeLatitude fromLngLat(double longitude,
                                               double latitude)
    {
        LongitudeLatitude res = new LongitudeLatitude();
        res.longitude = longitude;
        res.latitude  = latitude;
        return res;
    }

    public LongitudeLatitude computeDestination(double distance,
                                                double bearingInDegrees)
    {
        Point pt = LocationsEngine.toPoint(this);
        pt = TurfMeasurement.destination(pt, distance, bearingInDegrees, "meters");

        return fromLngLat(pt.longitude(), pt.latitude());
    }
}
