/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.location;

import java.util.List;

import com.optio3.cloud.hub.persistence.location.LocationRecord;
import com.optio3.cloud.model.TypedRecordIdentity;

public class LocationHierarchy
{
    public TypedRecordIdentity<LocationRecord> ri;
    public String                              name;
    public LocationType                        type;
    public List<LocationHierarchy>             subLocations;
}
