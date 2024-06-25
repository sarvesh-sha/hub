/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.alert;

import com.optio3.cloud.model.IEnumDescription;

public enum AlertStatus implements IEnumDescription
{
    active("Active", null),
    muted("Muted", null),
    resolved("Resolved", null),
    closed("Closed", null);

    private final String m_displayName;
    private final String m_description;

    AlertStatus(String displayName,
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
