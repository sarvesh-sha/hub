/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.workflow;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;
import com.optio3.cloud.hub.persistence.location.LocationRecord;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;

@JsonTypeName("WorkflowDetailsForSetLocationParent")
public class WorkflowDetailsForSetLocationParent extends WorkflowDetails implements IWorkflowHandler
{
    public String       parentLocationSysId;
    public List<String> childLocationSysIds = Lists.newArrayList();

    //--//

    @Override
    public WorkflowType resolveToType()
    {
        return WorkflowType.SetLocationParent;
    }

    @Override
    public void onCreate(SessionHolder sessionHolder)
    {
    }

    @Override
    public boolean postWorkflowCreation(SessionHolder sessionHolder)
    {
        RecordHelper<LocationRecord> helper = sessionHolder.createHelper(LocationRecord.class);

        if (parentLocationSysId == null)
        {
            for (String childLocationSysId : childLocationSysIds)
            {
                LocationRecord rec_child = helper.get(childLocationSysId);
                if (rec_child != null)
                {
                    rec_child.unlinkFromParent(helper);
                }
            }
        }
        else
        {
            LocationRecord rec_parent = helper.getOrNull(parentLocationSysId);
            if (rec_parent != null)
            {
                for (String childLocationSysId : childLocationSysIds)
                {
                    LocationRecord rec_child = helper.get(childLocationSysId);
                    if (rec_child != null)
                    {
                        rec_child.linkToParent(helper, rec_parent);
                    }
                }
            }
        }

        return true;
    }
}
