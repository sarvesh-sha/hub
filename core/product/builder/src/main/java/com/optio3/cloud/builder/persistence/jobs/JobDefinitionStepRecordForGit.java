/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.jobs;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.builder.model.jobs.JobDefinitionStepForGit;
import com.optio3.cloud.builder.orchestration.tasks.build.BaseBuildTask;
import com.optio3.cloud.builder.orchestration.tasks.build.TaskForGit;
import com.optio3.cloud.builder.persistence.jobs.input.RepositoryRecord;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Table(name = "JOB_DEF_STEP__GIT")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "JobDefinitionStepForGit", model = JobDefinitionStepForGit.class, metamodel = JobDefinitionStepRecordForGit_.class)
public class JobDefinitionStepRecordForGit extends JobDefinitionStepRecord
{
    @Optio3ControlNotifications(reason = "Ignore changes", direct = Notify.NEVER, reverse = Notify.NEVER)
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "repo", nullable = false, foreignKey = @ForeignKey(name = "REPO__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private RepositoryRecord repo;

    @Column(name = "directory", nullable = false)
    private String directory;

    //--//

    public JobDefinitionStepRecordForGit()
    {
    }

    public JobDefinitionStepRecordForGit(JobDefinitionRecord job)
    {
        super(job);
    }

    //--//

    public RepositoryRecord getRepo()
    {
        return repo;
    }

    public void setRepo(RepositoryRecord repo)
    {
        // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
        if (this.repo != repo)
        {
            this.repo = repo;
        }
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
        return TaskForGit.class;
    }
}
