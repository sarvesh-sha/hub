/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.location;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.model.asset.Asset;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.location.LocationRecord;

@JsonTypeName("Location")
public class Location extends Asset
{
    public LocationType      type;
    public String            phone;
    public String            address;
    public String            timeZone;
    public LongitudeLatitude geo;
    public List<GeoFence>    fences;

    //--//

    @Override
    public AssetRecord newRecord()
    {
        return new LocationRecord();
    }
}
