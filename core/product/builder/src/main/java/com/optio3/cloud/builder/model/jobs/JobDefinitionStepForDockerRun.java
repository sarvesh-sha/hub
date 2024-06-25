/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.jobs;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.cloud.annotation.Optio3MapToPersistence;
import com.optio3.cloud.builder.persistence.jobs.JobDefinitionRecord;
import com.optio3.cloud.builder.persistence.jobs.JobDefinitionStepRecord;
import com.optio3.cloud.builder.persistence.jobs.JobDefinitionStepRecordForDockerRun;

@JsonTypeName("JobDefinitionStepForDockerRun")
public class JobDefinitionStepForDockerRun extends JobDefinitionStep
{
    public String image;

    public String workingDirectory;

    public String commandLine;

    @Optio3MapToPersistence(value = "environmentVariables")
    public final Map<String, String> environmentVariables = Maps.newHashMap();

    @Optio3MapToPersistence(value = "bindings")
    public final Set<String> bindings = Sets.newHashSet();

    @Optio3MapToPersistence(value = "cdnSettings")
    public final List<JobDefinitionStepRecordForDockerRun.CdnContent> cdnSettings = Lists.newArrayList();

    //--//

    @Override
    public JobDefinitionStepRecord newRecord(JobDefinitionRecord jobDef)
    {
        return new JobDefinitionStepRecordForDockerRun(jobDef);
    }
}
