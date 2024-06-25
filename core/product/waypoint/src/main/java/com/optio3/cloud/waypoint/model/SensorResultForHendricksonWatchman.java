/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.waypoint.model;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("SensorResultForHendricksonWatchman")
public class SensorResultForHendricksonWatchman extends SensorResult
{
    public float temperature = Float.NaN;
    public float pressure    = Float.NaN;
}
