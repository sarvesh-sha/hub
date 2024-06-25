/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.asset;

import com.optio3.cloud.model.IEnumDescription;

public enum AssetState implements IEnumDescription
{
    provisioned("Provisioned", null),
    offline("Offline", null),
    passive("Passive", null),
    operational("Operational", null),
    maintenance("In Repair", null),
    retired("Retired", null);

    private final String m_displayName;
    private final String m_description;

    AssetState(String displayName,
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
