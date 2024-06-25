/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.deployment;

import java.time.ZonedDateTime;
import java.util.List;

import com.optio3.cloud.builder.model.jobs.JobStatus;
import com.optio3.cloud.model.SortCriteria;

public class DeploymentHostImagePullFilterRequest
{
    public String             hostSysId;
    public ZonedDateTime      olderThan;
    public ZonedDateTime      newerThan;
    public JobStatus          statusFilter;
    public List<SortCriteria> sortBy;
}
