/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.jobs;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.builder.model.jobs.JobDefinitionStepForMaven;
import com.optio3.cloud.builder.orchestration.tasks.build.BaseBuildTask;
import com.optio3.cloud.builder.orchestration.tasks.build.TaskForMaven;

@Entity
@Table(name = "JOB_DEF_STEP__MAVEN")
@Optio3TableInfo(externalId = "JobDefinitionStepForMaven", model = JobDefinitionStepForMaven.class, metamodel = JobDefinitionStepRecordForMaven_.class)
public class JobDefinitionStepRecordForMaven extends JobDefinitionStepRecord
{
    @Column(name = "pull_from", nullable = false)
    private String pullFrom;

    @Column(name = "directory", nullable = false)
    private String directory;

    //--//

    public JobDefinitionStepRecordForMaven()
    {
    }

    public JobDefinitionStepRecordForMaven(JobDefinitionRecord job)
    {
        super(job);
    }

    //--//

    public String getPullFrom()
    {
        return pullFrom;
    }

    public void setPullFrom(String pullFrom)
    {
        this.pullFrom = pullFrom;
    }

    public String getDirectory()
    {
        return directory;
    }

    public void setDirectory(String directory)
    {
        this.directory = directory;
    }

    //--//

    @Override
    public boolean requiresCdn()
    {
        return false;
    }

    @Override
    public Class<? extends BaseBuildTask> getHandler()
    {
        return TaskForMaven.class;
    }
}
