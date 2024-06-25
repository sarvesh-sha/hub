/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.waypoint.model;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("SensorResultForStealthPower")
public class SensorResultForStealthPower extends SensorResult
{
    public float supply_voltage = Float.NaN;
}
