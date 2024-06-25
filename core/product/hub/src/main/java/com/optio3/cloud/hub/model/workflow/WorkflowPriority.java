/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.workflow;

import com.optio3.cloud.model.IEnumDescription;

public enum WorkflowPriority implements IEnumDescription
{
    Urgent("Urgent", null, 1),
    High("High", null, 2),
    Normal("Normal", null, 3),
    Low("Low", null, 4);

    private final String m_displayName;
    private final String m_description;
    private final int    m_level;

    WorkflowPriority(String displayName,
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

    public boolean isMoreSevere(WorkflowPriority other)
    {
        return (other == null || getLevel() < other.getLevel());
    }

    public static WorkflowPriority parse(int value)
    {
        for (WorkflowPriority t : values())
        {
            if (t.m_level == value)
            {
                return t;
            }
        }

        return null;
    }
}
