/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.workflow;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;

@JsonTypeName("WorkflowDetailsForHidingControlPoint")
public class WorkflowDetailsForHidingControlPoint extends WorkflowDetails implements IWorkflowHandlerForCRE,
                                                                                     IWorkflowHandlerForOverrides
{
    public List<String> controlPoints = Lists.newArrayList();

    public boolean hide;

    //--//

    @Override
    public WorkflowType resolveToType()
    {
        return WorkflowType.HidingControlPoint;
    }

    @Override
    public void onCreate(SessionHolder sessionHolder)
    {
    }

    @Override
    public boolean postWorkflowCreationForCRE(SessionHolder sessionHolder)
    {
        RecordHelper<DeviceElementRecord> helper = sessionHolder.createHelper(DeviceElementRecord.class);

        for (String pointSysId : controlPoints)
        {
            DeviceElementRecord rec_point = helper.get(pointSysId);
            rec_point.setHidden(hide);
        }

        return true;
    }

    @Override
    public void getOverride(WorkflowOverrides overrides)
    {
    }
}
