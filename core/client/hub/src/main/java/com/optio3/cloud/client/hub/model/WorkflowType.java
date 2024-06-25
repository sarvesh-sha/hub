/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * Optio3 Hub APIs
 * APIs and Definitions for the Optio3 Hub product.
 *
 * OpenAPI spec version: 1.0.0
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.optio3.cloud.client.hub.model;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum WorkflowType
{
    RenameControlPoint(String.valueOf("RenameControlPoint")),
    SamplingControlPoint(String.valueOf("SamplingControlPoint")),
    SamplingPeriod(String.valueOf("SamplingPeriod")),
    HidingControlPoint(String.valueOf("HidingControlPoint")),
    AssignControlPointsToEquipment(String.valueOf("AssignControlPointsToEquipment")),
    SetControlPointsClass(String.valueOf("SetControlPointsClass")),
    IgnoreDevice(String.valueOf("IgnoreDevice")),
    RenameDevice(String.valueOf("RenameDevice")),
    SetDeviceLocation(String.valueOf("SetDeviceLocation")),
    RenameEquipment(String.valueOf("RenameEquipment")),
    RemoveEquipment(String.valueOf("RemoveEquipment")),
    MergeEquipments(String.valueOf("MergeEquipments")),
    NewEquipment(String.valueOf("NewEquipment")),
    SetEquipmentClass(String.valueOf("SetEquipmentClass")),
    SetEquipmentParent(String.valueOf("SetEquipmentParent")),
    SetEquipmentLocation(String.valueOf("SetEquipmentLocation")),
    SetLocationParent(String.valueOf("SetLocationParent"));

    private String value;

    WorkflowType(String v)
    {
        value = v;
    }

    public String value()
    {
        return value;
    }

    @Override
    public String toString()
    {
        return String.valueOf(value);
    }

    @JsonCreator
    public static WorkflowType fromValue(String v)
    {
        for (WorkflowType b : WorkflowType.values())
        {
            if (String.valueOf(b.value).equals(v))
                return b;
        }

        throw new IllegalArgumentException(String.format("'%s' is not one of the values accepted for %s class: %s", v, WorkflowType.class.getSimpleName(), Arrays.toString(WorkflowType.values())));
    }
}
