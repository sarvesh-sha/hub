/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.workflow;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;

@JsonTypeName("WorkflowDetailsForRenameControlPoint")
public class WorkflowDetailsForRenameControlPoint extends WorkflowDetails implements IWorkflowHandlerForCRE,
                                                                                     IWorkflowHandlerForOverrides
{
    public List<String> controlPoints = Lists.newArrayList();

    public String controlPointNewName;

    //--//

    public void setControlPointSysId(String controlPointSysId)
    {
        controlPoints.add(controlPointSysId);
    }

    public void setControlPointName(String controlPointName)
    {
    }

    //--//

    @Override
    public WorkflowType resolveToType()
    {
        return WorkflowType.RenameControlPoint;
    }

    @Override
    public void onCreate(SessionHolder sessionHolder)
    {
    }

    @Override
    public boolean postWorkflowCreationForCRE(SessionHolder sessionHolder)
    {
        RecordHelper<AssetRecord> helper = sessionHolder.createHelper(AssetRecord.class);

        for (String pointSysId : controlPoints)
        {
            AssetRecord rec_point = helper.get(pointSysId);
            rec_point.setNormalizedName(controlPointNewName);
        }

        return true;
    }

    @Override
    public void getOverride(WorkflowOverrides overrides)
    {
        for (String pointSysId : controlPoints)
        {
            overrides.pointNames.put(pointSysId, controlPointNewName);
        }
    }
}
