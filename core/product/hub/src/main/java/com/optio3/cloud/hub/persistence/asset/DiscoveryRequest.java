/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.asset;

import com.optio3.cloud.model.IEnumDescription;

public enum DiscoveryRequest implements IEnumDescription
{
    NONE("Not requested", null),
    INCREMENTAL("Request Incremental Processing", null),
    FULL("Request Full Processing", null);

    private final String m_displayName;
    private final String m_description;

    DiscoveryRequest(String displayName,
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
