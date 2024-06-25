/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.alert;

import com.optio3.cloud.model.IEnumDescription;

public enum AlertSampleAggregate implements IEnumDescription
{
    min("Minimum", null),
    max("Maximum", null),
    average("Average", null),
    delta("Delta", null),
    increment("Increment", null),
    decrement("Decrement", null);

    private final String m_displayName;
    private final String m_description;

    AlertSampleAggregate(String displayName,
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