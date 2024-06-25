/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.waypoint.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Maps;

@JsonTypeName("SensorResultForRawCANbus")
public class SensorResultForRawCANbus extends SensorResult
{
    public Map<Integer, Integer> found = Maps.newHashMap();
}
