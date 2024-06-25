/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.jobs;

import static java.util.Objects.requireNonNull;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.optio3.cloud.annotation.Optio3Cascade;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.builder.model.jobs.JobDefinitionStep;
import com.optio3.cloud.builder.orchestration.tasks.build.BaseBuildTask;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithMetadata;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "JOB_DEF_STEP")
@Optio3TableInfo(externalId = "JobDefinitionStep", model = JobDefinitionStep.class, metamodel = JobDefinitionStepRecord_.class)
public abstract class JobDefinitionStepRecord extends RecordWithMetadata implements ModelMapperTarget<JobDefinitionStep, JobDefinitionStepRecord_>
{
    /**
     * Bound to this job.
     */
    @Optio3ControlNotifications(reason = "Only notify job of definition's changes", direct = Notify.ALWAYS, reverse = Notify.NEVER, getter = "getOwningJob")
    @Optio3Cascade(mode = Optio3Cascade.Flavor.DELETE, getter = "getOwningJob")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "owning_job", nullable = false, foreignKey = @ForeignKey(name = "JOBDEFSTEP__OWNING_JOB__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private JobDefinitionRecord owningJob;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "build_id", nullable = false)
    private String buildId;

    @Column(name = "timeout", nullable = false)
    private int timeout;

    //--//

    protected JobDefinitionStepRecord()
    {
    }

    protected JobDefinitionStepRecord(JobDefinitionRecord job)
    {
        requireNonNull(job);

        owningJob = job;
    }

    //--//

    public JobDefinitionRecord getOwningJob()
    {
        return owningJob;
    }

    public int getPosition()
    {
        return position;
    }

    public void setPosition(int position)
    {
        this.position = position;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getBuildId()
    {
        return buildId;
    }

    public void setBuildId(String buildId)
    {
        this.buildId = buildId;
    }

    public int getTimeout()
    {
        return timeout;
    }

    public void setTimeout(int timeout)
    {
        this.timeout = timeout;
    }

    //--//

    public static List<JobDefinitionStepRecord> getBatch(RecordHelper<JobDefinitionStepRecord> helper,
                                                         List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }

    //--//

    public abstract boolean requiresCdn();

    public abstract Class<? extends BaseBuildTask> getHandler();
}
