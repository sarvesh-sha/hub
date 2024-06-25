/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.deployment;

import java.time.ZonedDateTime;
import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.util.CollectionUtils;

public class DeploymentHostOnlineSessions
{
    public final List<DeploymentHostOnlineSession> entries = Lists.newArrayList();

    public DeploymentHostOnlineSession accessLastSession(boolean onlyClosed)
    {
        int size = entries.size();
        if (size == 0)
        {
            return null;
        }

        DeploymentHostOnlineSession session = entries.get(size - 1);
        if (!onlyClosed || session.end != null)
        {
            return session;
        }

        return size > 1 ? entries.get(size - 2) : null;
    }

    public DeploymentHostOnlineSession accessLastSession()
    {
        return CollectionUtils.lastElement(entries);
    }

    public void prune(ZonedDateTime threshold)
    {
        entries.removeIf((entry) -> entry.end != null && entry.end.isBefore(threshold));
    }
}
