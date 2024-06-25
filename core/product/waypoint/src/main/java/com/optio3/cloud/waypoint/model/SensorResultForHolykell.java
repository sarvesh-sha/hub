/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.waypoint.model;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("SensorResultForHolykell")
public class SensorResultForHolykell extends SensorResult
{
    public float level       = Float.NaN;
    public float temperature = Float.NaN;
}
