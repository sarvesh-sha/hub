/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model;

import java.util.List;

import com.optio3.cloud.annotation.Optio3IncludeInApiDefinitions;
import com.optio3.cloud.hub.model.alert.AlertSeverity;
import com.optio3.cloud.hub.model.alert.AlertStatus;
import com.optio3.cloud.hub.model.alert.AlertType;
import com.optio3.cloud.hub.model.workflow.WorkflowPriority;
import com.optio3.cloud.hub.model.workflow.WorkflowStatus;
import com.optio3.cloud.hub.model.workflow.WorkflowType;
import com.optio3.cloud.hub.persistence.alert.AlertDefinitionRecord;
import com.optio3.cloud.model.TypedRecordIdentityList;
import org.apache.commons.lang3.StringUtils;

@Optio3IncludeInApiDefinitions
public class FilterPreferences
{
    public String                                         id;
    public String                                         name;
    public List<String>                                   locationIDs;
    public TypedRecordIdentityList<AlertDefinitionRecord> alertRules;
    public List<AlertStatus>                              alertStatusIDs;
    public List<AlertSeverity>                            alertSeverityIDs;
    public List<AlertType>                                alertTypeIDs;

    public String likeDeviceManufacturerName;
    public String likeDeviceProductName;
    public String likeDeviceModelName;

    public List<String> equipmentIDs;
    public List<String> equipmentClassIDs;

    public List<String>            deviceIDs;
    public List<String>            pointClassIDs;
    public FilterPreferenceBoolean isSampling;
    public FilterPreferenceBoolean isClassified;

    public List<String>           assignedToIDs;
    public List<String>           createdByIDs;
    public List<WorkflowType>     workflowTypeIDs;
    public List<WorkflowStatus>   workflowStatusIDs;
    public List<WorkflowPriority> workflowPriorityIDs;

    // TODO: UPGRADE PATCH: Legacy fixup for field type change.
    public void setSampling(String sampling)
    {
        if (StringUtils.equals(sampling, "Yes"))
        {
            isSampling = FilterPreferenceBoolean.Yes;
        }
        else if (StringUtils.equals(sampling, "No"))
        {
            isSampling = FilterPreferenceBoolean.No;
        }
    }

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setDeviceUnclassified(boolean unclassified)
    {
    }

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setDeviceIsUnclassified(FilterPreferenceBoolean deviceIsUnclassified)
    {
    }

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setDeviceCategoryIDs(List<String> deviceCategoryIDs)
    {
    }

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setDeviceProductNames(List<String> deviceProductNames)
    {
    }

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setDeviceManufacturerIDs(List<String> deviceManufacturerIDs)
    {
    }

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setDeviceModelNumbers(List<String> deviceModelNumbers)
    {
    }
}
