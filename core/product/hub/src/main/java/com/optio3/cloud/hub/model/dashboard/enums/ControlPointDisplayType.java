/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dashboard.enums;

import com.optio3.cloud.model.IEnumDescription;

public enum ControlPointDisplayType implements IEnumDescription
{
    NameOnly("Name only", null),
    LocationOnly("Location only", null),
    FullLocationOnly("Full location only", null),
    EquipmentOnly("Parent equipment only", null),

    NameLocation("Name, Location", null),
    LocationName("Location, Name", null),
    NameFullLocation("Name, Full location", null),
    FullLocationName("Full location, Name", null),

    NameEquipment("Name, Parent equipment", null),
    EquipmentName("Parent equipment, Name", null);

    private final String m_displayName;
    private final String m_description;

    ControlPointDisplayType(String displayName,
                            String description)
    {
        m_displayName = displayName;
        m_description = description;
    }

    @Override
    public String getDisplayName()
    {
        return this.m_displayName;
    }

    @Override
    public String getDescription()
    {
        return this.m_description;
    }
}
