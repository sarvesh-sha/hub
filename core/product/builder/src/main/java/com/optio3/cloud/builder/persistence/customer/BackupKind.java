/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.customer;

import com.optio3.cloud.model.IEnumDescription;

public enum BackupKind implements IEnumDescription
{
    HostMigration("Host Migration", "The backup was created to migrate services between hosts"),
    OnDemand("Manual", "The backup was created on-demand"),
    Upgrade("Upgrade", "The backup was created during an upgrade"),
    Hourly("Hourly", "A regular hourly backup"),
    Daily("Daily", "A regular daily backup");

    private final String m_displayName;
    private final String m_description;

    BackupKind(String displayName,
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
