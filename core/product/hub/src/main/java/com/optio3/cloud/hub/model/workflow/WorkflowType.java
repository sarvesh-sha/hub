/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.workflow;

import com.optio3.cloud.model.IEnumDescription;

public enum WorkflowType implements IEnumDescription
{
    RenameControlPoint("Rename control point", null),
    SamplingControlPoint("Sample control point", null),
    SamplingPeriod("Set sampling period for control point", null),
    HidingControlPoint("Hide control point", null),
    AssignControlPointsToEquipment("Assign control points to equipment", null),
    SetControlPointsClass("Set control points to class", null),
    //--//
    IgnoreDevice("Ignore device", null),
    RenameDevice("Rename device", null),
    SetDeviceLocation("Set device location", null),
    //--//
    RenameEquipment("Rename equipment", null),
    RemoveEquipment("Remove equipment", null),
    MergeEquipments("Merge equipments", null),
    NewEquipment("New equipment", null),
    SetEquipmentClass("Set equipment class", null),
    SetEquipmentParent("Set equipment parent", null),
    SetEquipmentLocation("Set equipment location", null),
    //--//
    SetLocationParent("Set location parent", null);

    private final String m_displayName;
    private final String m_description;

    WorkflowType(String displayName,
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
