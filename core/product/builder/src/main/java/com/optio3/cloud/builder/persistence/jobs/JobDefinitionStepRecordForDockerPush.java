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
import com.optio3.cloud.builder.model.jobs.JobDefinitionStepForDockerPush;
import com.optio3.cloud.builder.orchestration.tasks.build.BaseBuildTask;
import com.optio3.cloud.builder.orchestration.tasks.build.TaskForDockerPush;

@Entity
@Table(name = "JOB_DEF_STEP__DOCKER_PUSH")
@Optio3TableInfo(externalId = "JobDefinitionStepForDockerPush", model = JobDefinitionStepForDockerPush.class, metamodel = JobDefinitionStepRecordForDockerPush_.class)
public class JobDefinitionStepRecordForDockerPush extends JobDefinitionStepRecord
{
    @Column(name = "source_image", nullable = false)
    private String sourceImage;

    @Column(name = "image_tag", nullable = false)
    private String imageTag;

    //--//

    public JobDefinitionStepRecordForDockerPush()
    {
    }

    public JobDefinitionStepRecordForDockerPush(JobDefinitionRecord job)
    {
        super(job);
    }

    //--//

    public String getSourceImage()
    {
        return sourceImage;
    }

    public void setSourceImage(String targetImage)
    {
        this.sourceImage = targetImage;
    }

    public String getImageTag()
    {
        return imageTag;
    }

    public void setImageTag(String imageTag)
    {
        this.imageTag = imageTag;
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
        return TaskForDockerPush.class;
    }
}
