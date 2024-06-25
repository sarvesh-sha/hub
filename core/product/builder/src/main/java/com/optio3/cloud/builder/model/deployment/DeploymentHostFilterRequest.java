/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.deployment;

import java.util.List;

import com.optio3.cloud.model.SortCriteria;

public class DeploymentHostFilterRequest
{
    public boolean                        includeFullDetails;
    public String                         serviceSysid;
    public String                         likeFilter;
    public DeploymentHostFilterStatusPair statusFilter;
    public List<SortCriteria>             sortBy;
}
