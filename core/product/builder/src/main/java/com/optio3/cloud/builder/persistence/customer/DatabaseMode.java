/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.customer;

import com.optio3.cloud.model.IEnumDescription;

public enum DatabaseMode implements IEnumDescription
{
    None("No database", null),
    H2InMemory("H2 In Memory", null),
    H2OnDisk("H2 On Disk", null),
    MariaDB("MariaDB", null);

    private final String m_displayName;
    private final String m_description;

    DatabaseMode(String displayName,
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
