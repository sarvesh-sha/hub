/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model;

import java.util.List;

import com.google.common.collect.Lists;

public class ControllerMetadataAggregation
{
    public String sysId;
    public String name;
    public int    networkNumber;
    public int    instanceNumber;

    public List<MetadataAggregationPoint> points = Lists.newArrayList();
}
