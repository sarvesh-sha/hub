/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.workflow;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.optio3.cloud.persistence.SessionHolder;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes({ @JsonSubTypes.Type(value = WorkflowDetailsForAssignControlPointsToEquipment.class),
                @JsonSubTypes.Type(value = WorkflowDetailsForHidingControlPoint.class),
                @JsonSubTypes.Type(value = WorkflowDetailsForIgnoreDevice.class),
                @JsonSubTypes.Type(value = WorkflowDetailsForSetLocationParent.class),
                @JsonSubTypes.Type(value = WorkflowDetailsForMergeEquipments.class),
                @JsonSubTypes.Type(value = WorkflowDetailsForNewEquipment.class),
                @JsonSubTypes.Type(value = WorkflowDetailsForRemoveEquipment.class),
                @JsonSubTypes.Type(value = WorkflowDetailsForRenameControlPoint.class),
                @JsonSubTypes.Type(value = WorkflowDetailsForRenameDevice.class),
                @JsonSubTypes.Type(value = WorkflowDetailsForRenameEquipment.class),
                @JsonSubTypes.Type(value = WorkflowDetailsForSamplingControlPoint.class),
                @JsonSubTypes.Type(value = WorkflowDetailsForSamplingPeriod.class),
                @JsonSubTypes.Type(value = WorkflowDetailsForSetControlPointsClass.class),
                @JsonSubTypes.Type(value = WorkflowDetailsForSetDeviceLocation.class),
                @JsonSubTypes.Type(value = WorkflowDetailsForSetEquipmentClass.class),
                @JsonSubTypes.Type(value = WorkflowDetailsForSetEquipmentLocation.class),
                @JsonSubTypes.Type(value = WorkflowDetailsForSetEquipmentParent.class) })
public abstract class WorkflowDetails
{
    public abstract WorkflowType resolveToType();

    public abstract void onCreate(SessionHolder sessionHolder);
}

