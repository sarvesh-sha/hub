/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.metrics;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.cloud.model.SortCriteria;

public class MetricsDefinitionFilterRequest
{
    public List<SortCriteria> sortBy = Lists.newArrayList();
}
