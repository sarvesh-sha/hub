/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.alert;

import com.optio3.cloud.model.IEnumDescription;

public enum AlertSeverity implements IEnumDescription
{
    CRITICAL("Critical", null, 1),
    SIGNIFICANT("Significant", null, 2),
    NORMAL("Minor impact", null, 3),
    LOW("Informational", null, 4);

    private final String m_displayName;
    private final String m_description;
    private final int    m_level;

    AlertSeverity(String displayName,
                  String description,
                  int level)
    {
        m_displayName = displayName;
        m_description = description;
        m_level       = level;
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

    //--//

    public int getLevel()
    {
        return m_level;
    }

    public boolean isMoreSevere(AlertSeverity other)
    {
        return (other == null || getLevel() < other.getLevel());
    }

    public static AlertSeverity parse(int value)
    {
        for (AlertSeverity t : values())
        {
            if (t.m_level == value)
            {
                return t;
            }
        }

        return null;
    }
}
