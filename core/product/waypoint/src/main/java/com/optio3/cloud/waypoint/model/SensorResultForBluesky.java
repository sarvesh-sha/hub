/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.waypoint.model;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("SensorResultForBluesky")
public class SensorResultForBluesky extends SensorResult
{
    public float inputVoltage   = Float.NaN;
    public float inputCurrent   = Float.NaN;
    public float batteryVoltage = Float.NaN;
    public float batteryCurrent = Float.NaN;
    public float totalChargeAH  = Float.NaN;
}
