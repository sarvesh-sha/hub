/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.workflow;

import com.optio3.cloud.model.IEnumDescription;

public enum WorkflowEventType implements IEnumDescription
{
    created("Created", "New workflow"),
    updatedWithNotes("Updated with notes", "User added notes"),
    reassigned("Reassigned", "Workflow was reassigned to a different user"),
    resolved("Resolved", "Workflow was resolved"),
    closed("Closed", "Workflow was closed"),
    reopened("Reopened", "Workflow was reactivated"),
    disabling("Disabling", "Disabling workflow"),
    disabled("Disabled", "Workflow was disabled");

    private final String m_displayName;
    private final String m_description;

    WorkflowEventType(String displayName,
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
