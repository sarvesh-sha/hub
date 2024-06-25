/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.waypoint.model;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Sets;

@JsonTypeName("SensorResultForJ1939")
public class SensorResultForJ1939 extends SensorResult
{
    public Set<String> found = Sets.newHashSet();
}
