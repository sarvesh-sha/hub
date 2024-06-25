/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.model.scheduler;

import java.util.Collection;

import com.optio3.cloud.logic.BackgroundActivityStatus;

public class BackgroundActivityFilterPair
{
    public BackgroundActivityFilter             filter;
    public Collection<BackgroundActivityStatus> targets;

    public static BackgroundActivityFilterPair build(Collection<BackgroundActivityStatus> targets)
    {
        BackgroundActivityFilterPair res = new BackgroundActivityFilterPair();
        res.filter = BackgroundActivityFilter.matchingStatus;
        res.targets = targets;
        return res;
    }
}
