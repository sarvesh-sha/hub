/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.report;

import com.optio3.cloud.model.IEnumDescription;

public enum ReportFrequency implements IEnumDescription
{
    NoRepeat("On-Demand", null),
    Daily("Daily", null),
    Weekly("Weekly", null),
    Monthly("Monthly", null),
    Quarterly("Quarterly", null);

    private final String m_displayName;
    private final String m_description;

    ReportFrequency(String displayName,
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
