/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dashboard;

import java.util.List;

import com.google.common.collect.Lists;

public class AggregationResponse
{
    public List<String[]> records         = Lists.newArrayList();
    public List<double[]> resultsPerRange = Lists.newArrayList();
}
