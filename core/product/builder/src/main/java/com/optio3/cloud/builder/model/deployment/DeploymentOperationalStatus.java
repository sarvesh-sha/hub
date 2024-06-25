/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.deployment;

import com.optio3.cloud.model.IEnumDescription;

public enum DeploymentOperationalStatus implements IEnumDescription
{
    factoryFloor("On Factory Floor", null, false, false, true, false),
    provisioned("Provisioned", null, false, false, true, false),
    installationPending("Installation Pending", null, false, false, false, false),
    offline("Offline", null, false, false, false, false),
    idle("Idle", null, null, true, false, false),
    operational("Operational", null, true, true, false, false),
    maintenance("In Repair", null, false, false, false, true),
    lostConnectivity("Lost Connectivity", null, false, false, true, true),
    storageCorruption("Storage Corruption", null, null, false, true, true),
    RMA_warranty("RMA - Warranty", null, false, false, true, false),
    RMA_nowarranty("RMA - No Warranty", null, false, false, true, false),
    retired("Retired", null, false, false, true, false);

    private final String  m_displayName;
    private final String  m_description;
    private final Boolean m_shouldBeResponsive;
    private final boolean m_acceptsNewTasks;
    private final boolean m_ignoreOldTasks;
    private final boolean m_malfunctioning;

    DeploymentOperationalStatus(String displayName,
                                String description,
                                Boolean shouldBeResponsive,
                                boolean acceptsNewTasks,
                                boolean ignoreOldTasks,
                                boolean malfunctioning)
    {
        m_displayName        = displayName;
        m_description        = description;
        m_shouldBeResponsive = shouldBeResponsive;
        m_acceptsNewTasks    = acceptsNewTasks;
        m_ignoreOldTasks     = ignoreOldTasks;
        m_malfunctioning     = malfunctioning;
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

    public Boolean shouldBeResponsive()
    {
        return m_shouldBeResponsive;
    }

    public boolean acceptsNewTasks()
    {
        return m_acceptsNewTasks;
    }

    public boolean canIgnoreOldTasks()
    {
        return m_ignoreOldTasks;
    }

    public boolean isMalfunctioning()
    {
        return m_malfunctioning;
    }
}
