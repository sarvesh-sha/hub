/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.alert;

import com.optio3.cloud.model.IEnumDescription;

public enum AlertType implements IEnumDescription
{
    ALARM("Alarm", null),
    COMMUNICATION_PROBLEM("Communication problem", null),
    DEVICE_FAILURE("Device Failure", null),
    END_OF_LIFE("End of life", null),
    INFORMATIONAL("Informational", null),
    OPERATOR_SUMMARY("Operator Summary", null),
    RECALL("Recall", null),
    THRESHOLD_EXCEEDED("Threshold exceeded", null),
    WARNING("Warning", null),
    WARRANTY("Warranty", null);

    private final String m_displayName;
    private final String m_description;

    AlertType(String displayName,
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
