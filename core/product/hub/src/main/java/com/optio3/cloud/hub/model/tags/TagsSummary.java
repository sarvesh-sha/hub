/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.tags;

import java.util.Map;

import com.google.common.collect.Maps;

public class TagsSummary
{
    public int version;

    public Map<String, Integer> tagFrequency      = Maps.newHashMap();
    public Map<String, Integer> relationFrequency = Maps.newHashMap();

    public Map<String, Integer> pointClassesFrequency     = Maps.newHashMap();
    public Map<String, Integer> equipmentClassesFrequency = Maps.newHashMap();
}
