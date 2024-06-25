/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.jobs.input;

import com.optio3.cloud.model.scheduler.BaseBackgroundActivityProgress;

public class RepositoryRefresh extends BaseBackgroundActivityProgress
{
    public int branchesAdded;
    public int branchesRemoved;

    public int commitsAdded;
    public int commitsRemoved;
}
