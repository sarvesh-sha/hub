/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.alert;

import com.optio3.cloud.model.IEnumDescription;

public enum AlertEventType implements IEnumDescription
{
    created("Created", "New alert"),
    updatedWithNotes("Updated with notes", "User added notes"),
    reassigned("Reassigned", "Alert was reassigned to a different user"),
    muted("Muted", "Alert was muted"),
    unmuted("Unmuted", "Alert was unmuted"),
    resolved("Resolved", "Alert was resolved"),
    closed("Closed", "Alert was closed"),
    reopened("Reopened", "Alert was reactivated");

    private final String m_displayName;
    private final String m_description;

    AlertEventType(String displayName,
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
