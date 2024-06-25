/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.workflow;

import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.hub.persistence.workflow.WorkflowRecord;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.model.TypedRecordIdentity;

public class WorkflowHistory extends BaseModel
{
    @Optio3MapAsReadOnly
    public TypedRecordIdentity<WorkflowRecord> workflow;

    @Optio3MapAsReadOnly
    public WorkflowEventType type;

    @Optio3MapAsReadOnly
    public String text;

    @Optio3MapAsReadOnly
    public TypedRecordIdentity<UserRecord> user;
}
