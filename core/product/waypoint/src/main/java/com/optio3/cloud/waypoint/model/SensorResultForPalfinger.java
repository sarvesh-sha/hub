/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.waypoint.model;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("SensorResultForPalfinger")
public class SensorResultForPalfinger extends SensorResult
{
    public float counterService = Float.NaN;
    public float supplyVoltage  = Float.NaN;
    public float plcTemperature = Float.NaN;
}
