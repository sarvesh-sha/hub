/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.waypoint.model;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("SensorResultForZeroRPM")
public class SensorResultForZeroRPM extends SensorResult
{
    public String vin;
    public float  keyLifeHours = Float.NaN;
}
