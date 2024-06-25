/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.workflow;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.hub.model.event.Event;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.model.TypedRecordIdentity;

@JsonTypeName("Workflow")
public class Workflow extends Event
{
    @Optio3MapAsReadOnly
    public TypedRecordIdentity<UserRecord> createdBy;

    @Optio3MapAsReadOnly
    public TypedRecordIdentity<UserRecord> assignedTo;

    @Optio3MapAsReadOnly // Handled explicitly.
    public WorkflowStatus status;

    @Optio3MapAsReadOnly
    public WorkflowType type;

    public WorkflowPriority priority;

    @Optio3MapAsReadOnly
    public WorkflowDetails details;
}
