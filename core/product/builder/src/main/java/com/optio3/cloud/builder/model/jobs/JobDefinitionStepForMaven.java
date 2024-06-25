/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.jobs;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.builder.persistence.jobs.JobDefinitionRecord;
import com.optio3.cloud.builder.persistence.jobs.JobDefinitionStepRecord;
import com.optio3.cloud.builder.persistence.jobs.JobDefinitionStepRecordForMaven;

@JsonTypeName("JobDefinitionStepForMaven")
public class JobDefinitionStepForMaven extends JobDefinitionStep
{
    public String pullFrom;

    public String directory;

    //--//

    @Override
    public JobDefinitionStepRecord newRecord(JobDefinitionRecord jobDef)
    {
        return new JobDefinitionStepRecordForMaven(jobDef);
    }
}
