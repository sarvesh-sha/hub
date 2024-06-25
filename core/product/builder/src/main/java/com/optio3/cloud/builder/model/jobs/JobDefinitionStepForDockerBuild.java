/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.jobs;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Maps;
import com.optio3.cloud.annotation.Optio3MapToPersistence;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.builder.persistence.jobs.JobDefinitionRecord;
import com.optio3.cloud.builder.persistence.jobs.JobDefinitionStepRecord;
import com.optio3.cloud.builder.persistence.jobs.JobDefinitionStepRecordForDockerBuild;

@JsonTypeName("JobDefinitionStepForDockerBuild")
public class JobDefinitionStepForDockerBuild extends JobDefinitionStep
{
    public String sourcePath;

    public String dockerFile;

    public DeploymentRole targetService;

    @Optio3MapToPersistence(value = "buildArgs", useGetterForUpdate = true)
    public Map<String, String> buildArgs = Maps.newHashMap();

    //--//

    @Override
    public JobDefinitionStepRecord newRecord(JobDefinitionRecord jobDef)
    {
        return new JobDefinitionStepRecordForDockerBuild(jobDef);
    }
}
