/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.workflow;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;

@JsonTypeName("WorkflowDetailsForSetControlPointsClass")
public class WorkflowDetailsForSetControlPointsClass extends WorkflowDetails implements IWorkflowHandlerForCRE,
                                                                                        IWorkflowHandlerForOverrides
{
    public String pointClassId;

    public List<String> controlPoints;

    //--//

    @Override
    public WorkflowType resolveToType()
    {
        return WorkflowType.SetControlPointsClass;
    }

    @Override
    public void onCreate(SessionHolder sessionHolder)
    {
    }

    @Override
    public boolean postWorkflowCreationForCRE(SessionHolder sessionHolder)
    {
        RecordHelper<AssetRecord> helper = sessionHolder.createHelper(AssetRecord.class);

        for (String id : controlPoints)
        {
            AssetRecord rec_element = helper.get(id);
            rec_element.setPointClassId(pointClassId);
        }

        return true;
    }

    @Override
    public void getOverride(WorkflowOverrides overrides)
    {
        for (String sysId : controlPoints)
        {
            overrides.pointClasses.put(sysId, pointClassId);
        }
    }
}
