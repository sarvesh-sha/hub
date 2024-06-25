/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.waypoint.model;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Sets;

@JsonTypeName("SensorResultForGps")
public class SensorResultForGps extends SensorResult
{
    public double latitude  = Double.NaN;
    public double longitude = Double.NaN;
    public float  altitude  = Float.NaN;
    public float  speed     = Float.NaN;

    public boolean      hasFix;
    public Set<Integer> satellitesInFix  = Sets.newHashSet();
    public Set<Integer> satellitesInView = Sets.newHashSet();
}
