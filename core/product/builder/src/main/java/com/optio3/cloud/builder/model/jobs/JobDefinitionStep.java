/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.jobs;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.builder.persistence.jobs.JobDefinitionRecord;
import com.optio3.cloud.builder.persistence.jobs.JobDefinitionStepRecord;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.model.TypedRecordIdentity;

@JsonTypeInfo(use = Id.NAME, property = "__type")
@JsonSubTypes({ @Type(value = JobDefinitionStepForGit.class),
                @Type(value = JobDefinitionStepForMaven.class),
                @Type(value = JobDefinitionStepForDockerRun.class),
                @Type(value = JobDefinitionStepForDockerBuild.class),
                @Type(value = JobDefinitionStepForDockerPush.class),
                @Type(value = JobDefinitionStepForSshCommand.class) })
public abstract class JobDefinitionStep extends BaseModel
{
    public String buildId;

    //--//

    @Optio3MapAsReadOnly
    public TypedRecordIdentity<JobDefinitionRecord> owningJob;

    //--//

    public int position;

    public String name;

    public int timeout;

    //--//

    public abstract JobDefinitionStepRecord newRecord(JobDefinitionRecord jobDef);
}
