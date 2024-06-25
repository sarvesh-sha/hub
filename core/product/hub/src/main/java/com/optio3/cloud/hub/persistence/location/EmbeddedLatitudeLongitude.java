/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.location;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import com.optio3.cloud.hub.model.location.LongitudeLatitude;
import com.optio3.serialization.Reflection;

@Embeddable
public class EmbeddedLatitudeLongitude
{
    @Column(nullable = true)
    private double latitude;

    @Column(nullable = true)
    private double longitude;

    //--//

    public double getLatitude()
    {
        return latitude;
    }

    public void setLatitude(double latitude)
    {
        this.latitude = latitude;
    }

    public double getLongitude()
    {
        return longitude;
    }

    public void setLongitude(double longitude)
    {
        this.longitude = longitude;
    }

    //--//

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        EmbeddedLatitudeLongitude that = Reflection.as(o, EmbeddedLatitudeLongitude.class);
        if (that == null)
        {
            return false;
        }

        return Double.compare(that.latitude, latitude) == 0 && Double.compare(that.longitude, longitude) == 0;
    }

    @Override
    public int hashCode()
    {
        int result = 1;

        result = 31 * result + Double.hashCode(latitude);
        result = 31 * result + Double.hashCode(longitude);

        return result;
    }

    //--//

    public LongitudeLatitude convert()
    {
        if (latitude != 0.0 || longitude != 0.0)
        {
            LongitudeLatitude newGeo = new LongitudeLatitude();
            newGeo.longitude = longitude;
            newGeo.latitude  = latitude;
            return newGeo;
        }

        return null;
    }
}
