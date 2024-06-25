/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.block;

import com.optio3.cloud.model.IEnumDescription;

public enum AlertEngineSampleProperty implements IEnumDescription
{
    PresentValue("Present Value", null),
    OutOfService("Out of service", null),
    InAlarm("In Alarm", null);

    private final String m_displayName;

    private final String m_description;

    AlertEngineSampleProperty(String displayName,
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
