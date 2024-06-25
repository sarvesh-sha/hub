/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.search;

import java.util.List;

import com.google.common.collect.Lists;

public class SearchRequest
{
    public String query;

    public List<SearchRequestFilters> filters = Lists.newArrayList();

    public boolean scopeToFilters;
}
