/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.jobs;

import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.builder.persistence.jobs.JobDefinitionStepRecord;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.model.TypedRecordIdentityList;

public class JobDefinition extends BaseModel
{
    public String name;

    public String idPrefix;

    public int totalTimeout;

    //--//

    @Optio3MapAsReadOnly
    public TypedRecordIdentityList<JobDefinitionStepRecord> steps = new TypedRecordIdentityList<>();
}
