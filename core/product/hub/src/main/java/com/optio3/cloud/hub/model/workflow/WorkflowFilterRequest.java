/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.workflow;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.model.event.EventFilterRequest;

@JsonTypeName("WorkflowFilterRequest")
public class WorkflowFilterRequest extends EventFilterRequest
{
    public String                 likeFilter;
    public List<WorkflowStatus>   workflowStatusIDs;
    public List<WorkflowType>     workflowTypeIDs;
    public List<WorkflowPriority> workflowPriorityIDs;
    public List<String>           createdByIDs;
    public List<String>           assignedToIDs;

    //--//

    public boolean hasStatus()
    {
        return hasItems(workflowStatusIDs);
    }

    public boolean hasTypes()
    {
        return hasItems(workflowTypeIDs);
    }

    public boolean hasPriorities()
    {
        return hasItems(workflowPriorityIDs);
    }

    public boolean hasCreatedBy()
    {
        return hasItems(createdByIDs);
    }

    public boolean hasAssignedTo()
    {
        return hasItems(assignedToIDs);
    }
}
