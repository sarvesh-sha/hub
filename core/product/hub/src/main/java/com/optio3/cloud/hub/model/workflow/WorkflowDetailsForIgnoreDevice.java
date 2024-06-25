/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.workflow;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.persistence.SessionHolder;

@JsonTypeName("WorkflowDetailsForIgnoreDevice")
public class WorkflowDetailsForIgnoreDevice extends WorkflowDetails implements IWorkflowHandlerForCRE,
                                                                               IWorkflowHandlerForOverrides
{
    public String deviceSysId;
    public String deviceName;

    //--//

    @Override
    public WorkflowType resolveToType()
    {
        return WorkflowType.IgnoreDevice;
    }

    @Override
    public void onCreate(SessionHolder sessionHolder)
    {
    }

    @Override
    public boolean postWorkflowCreationForCRE(SessionHolder holder)
    {
        return false;
    }

    @Override
    public void getOverride(WorkflowOverrides overrides)
    {
    }
}
