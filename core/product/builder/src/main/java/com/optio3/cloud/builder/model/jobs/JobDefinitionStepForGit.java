/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.jobs;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.builder.persistence.jobs.JobDefinitionRecord;
import com.optio3.cloud.builder.persistence.jobs.JobDefinitionStepRecord;
import com.optio3.cloud.builder.persistence.jobs.JobDefinitionStepRecordForGit;
import com.optio3.cloud.builder.persistence.jobs.input.RepositoryRecord;
import com.optio3.cloud.model.TypedRecordIdentity;

@JsonTypeName("JobDefinitionStepForGit")
public class JobDefinitionStepForGit extends JobDefinitionStep
{
    public TypedRecordIdentity<RepositoryRecord> repo;

    public String directory;

    //--//

    @Override
    public JobDefinitionStepRecord newRecord(JobDefinitionRecord jobDef)
    {
        return new JobDefinitionStepRecordForGit(jobDef);
    }
}
