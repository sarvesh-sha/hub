/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.waypoint.model;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("SensorResultForBergstrom")
public class SensorResultForBergstrom extends SensorResult
{
    public float compressorSpeed = Float.NaN;
    public float stateOfCharge   = Float.NaN;
    public float voltage         = Float.NaN;
}
