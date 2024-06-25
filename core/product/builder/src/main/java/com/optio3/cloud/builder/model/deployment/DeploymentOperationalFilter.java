/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.deployment;

import com.optio3.cloud.model.IEnumDescription;

public enum DeploymentOperationalFilter implements IEnumDescription
{
    all("All States", null),
    recentlyCreated("Only recently created", null),
    delayedOps("Only with delayed operations", null),
    nonActiveAgents("Only with non-active agents", null),
    stoppedTasks("Only with stopped tasks", null),
    needAttention("Only needing attention", null),
    onlyWaypoint("Only with waypoint", null),
    onlyBroken("Only broken", null),
    matchingStatus("Matching status", null);

    private final String m_displayName;
    private final String m_description;

    DeploymentOperationalFilter(String displayName,
                                String description)
    {
        m_displayName = displayName;
        m_description = description;
    }

    @Override
    public String getDisplayName()
    {
        return m_displayName;
    }

    @Override
    public String getDescription()
    {
        return m_description;
    }
}
