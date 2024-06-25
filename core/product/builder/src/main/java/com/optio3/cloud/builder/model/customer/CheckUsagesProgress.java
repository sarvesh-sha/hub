/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.customer;

import com.optio3.cloud.model.scheduler.BaseBackgroundActivityProgress;

public class CheckUsagesProgress extends BaseBackgroundActivityProgress
{
    public com.optio3.cloud.client.hub.model.UsageFilterResponse results;
}
