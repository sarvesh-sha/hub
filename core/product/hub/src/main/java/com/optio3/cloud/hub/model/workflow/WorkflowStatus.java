/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.workflow;

import com.optio3.cloud.model.IEnumDescription;

public enum WorkflowStatus implements IEnumDescription
{
    Active("Active", null),
    Resolved("Resolved", null),
    Closed("Closed", null),
    Disabling("Disabling", null),
    Disabled("Disabled", null);

    private final String m_displayName;
    private final String m_description;

    WorkflowStatus(String displayName,
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
