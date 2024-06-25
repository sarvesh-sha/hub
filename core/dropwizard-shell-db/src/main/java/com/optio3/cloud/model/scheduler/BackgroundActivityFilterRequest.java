/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.model.scheduler;

import java.util.List;

import com.optio3.cloud.model.SortCriteria;

public class BackgroundActivityFilterRequest
{
    public boolean                      onlyReadyToGo;
    public String                       likeFilter;
    public BackgroundActivityFilterPair statusFilter;
    public List<SortCriteria>           sortBy;
}
