/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.waypoint.model;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("SensorResultForTriStar")
public class SensorResultForTriStar extends SensorResult
{
    public float batteryVoltage = Float.NaN;
    public float batteryCurrent = Float.NaN;
    public float arrayVoltage   = Float.NaN;
    public float arrayCurrent   = Float.NaN;
}
