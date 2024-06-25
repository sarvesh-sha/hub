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
import com.optio3.util.TimeUtils;

public class DeploymentCellularSessions
{
    public List<DeploymentCellularSession> sessions = Lists.newArrayList();

    public DeploymentCellularSession ensureTimestamp(ZonedDateTime timestamp)
    {
        if (timestamp == null)
        {
            return null;
        }

        DeploymentCellularSession res = CollectionUtils.findFirst(sessions, (session) -> TimeUtils.compare(session.start, timestamp) == 0);
        if (res == null)
        {
            res = new DeploymentCellularSession();
            res.start = timestamp;
            sessions.add(res);
        }

        return res;
    }
}
