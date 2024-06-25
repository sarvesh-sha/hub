/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.model.scheduler;

import com.optio3.cloud.model.IEnumDescription;

public enum BackgroundActivityFilter implements IEnumDescription
{
    all("All States", null),
    hideCompleted("Still processing or failed", null),
    running("Still processing", null),
    completed("Done processing", null),
    matchingStatus("Matching status", null);

    private final String m_displayName;
    private final String m_description;

    BackgroundActivityFilter(String displayName,
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
