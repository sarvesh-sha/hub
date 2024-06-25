/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.jobs;

import static java.util.Objects.requireNonNull;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.optio3.cloud.annotation.Optio3Cascade;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.builder.HostRemoter;
import com.optio3.cloud.builder.model.jobs.JobStatus;
import com.optio3.cloud.builder.model.jobs.JobStep;
import com.optio3.cloud.builder.persistence.worker.DockerContainerRecord;
import com.optio3.cloud.builder.persistence.worker.HostRecord;
import com.optio3.cloud.builder.persistence.worker.RecordWithResources;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.LogHandler;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Table(name = "JOB_STEP", indexes = { @Index(columnList = "status") })
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "JobStep", model = JobStep.class, metamodel = JobStepRecord_.class)
public class JobStepRecord extends RecordWithResources implements ModelMapperTarget<JobStep, JobStepRecord_>,
                                                                  LogHandler.ILogHost<JobStepLogRecord>
{
    /**
     * Bound to this job.
     */
    @Optio3ControlNotifications(reason = "Notify job of step's changes", direct = Notify.ALWAYS, reverse = Notify.NEVER, getter = "getOwningJob")
    @Optio3Cascade(mode = Optio3Cascade.Flavor.DELETE, getter = "getOwningJob")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "owning_job", nullable = false, foreignKey = @ForeignKey(name = "JOBSTEP__OWNING_JOB__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private JobRecord owningJob;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private JobStatus status;

    //--//

    @Column(name = "last_output")
    private ZonedDateTime lastOutput;

    @Column(name = "last_offset", nullable = false)
    private int lastOffset;

    @Lob
    @Column(name = "log_ranges")
    private byte[] logRanges;

    //--//

    /**
     * The target command.
     */
    @Optio3ControlNotifications(reason = "Ignore changes", direct = Notify.NEVER, reverse = Notify.NEVER)
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "container", foreignKey = @ForeignKey(name = "CONTAINER__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private DockerContainerRecord container;

    //--//

    public JobStepRecord()
    {
    }

    public static JobStepRecord newInstance(JobRecord job)
    {
        requireNonNull(job);

        JobStepRecord res = new JobStepRecord();
        res.owningJob = job;
        res.status    = JobStatus.CREATED;
        return res;
    }

    //--//

    public ZonedDateTime getLastOutput()
    {
        return lastOutput;
    }

    public int getLastOffset()
    {
        return lastOffset;
    }

    @Override
    public byte[] getLogRanges()
    {
        return logRanges;
    }

    @Override
    public void setLogRanges(byte[] logRanges,
                             ZonedDateTime lastOutput,
                             int lastOffset)
    {
        if (!Arrays.equals(this.logRanges, logRanges))
        {
            this.logRanges  = logRanges;
            this.lastOutput = lastOutput;
            this.lastOffset = lastOffset;
        }
    }

    @Override
    public void refineLogQuery(LogHandler.JoinHelper<?, JobStepLogRecord> jh)
    {
        jh.addWhereClauseWithEqual(jh.rootLog, JobStepLogRecord_.owningJobStep, this);
    }

    @Override
    public JobStepLogRecord allocateNewLogInstance()
    {
        return JobStepLogRecord.newInstance(this);
    }

    public static LogHandler<JobStepRecord, JobStepLogRecord> allocateLogHandler(RecordLocked<JobStepRecord> lock)
    {
        return new LogHandler<>(lock, JobStepLogRecord.class);
    }

    public static LogHandler<JobStepRecord, JobStepLogRecord> allocateLogHandler(SessionHolder sessionHolder,
                                                                                 JobStepRecord rec)
    {
        return new LogHandler<>(sessionHolder, rec, JobStepLogRecord.class);
    }

    //--//

    public HostRecord getOwningHost()
    {
        return container != null ? container.getOwningHost() : null;
    }

    public JobRecord getOwningJob()
    {
        return owningJob;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public JobStatus getStatus()
    {
        return status;
    }

    public void setStatus(JobStatus status)
    {
        this.status = status;
    }

    public DockerContainerRecord getContainer()
    {
        return container;
    }

    public void setContainer(DockerContainerRecord container)
    {
        // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
        if (this.container != container)
        {
            this.container = container;
        }
    }

    public HostRecord getBoundHost()
    {
        return container != null ? container.getOwningHost() : null;
    }

    //--//

    public static List<JobStepRecord> getBatch(RecordHelper<JobStepRecord> helper,
                                               List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }

    //--//

    @Override
    protected void checkConditionsForFreeingResourcesInner(ValidationResultsHolder validation)
    {
        // Nothing to check.
    }

    @Override
    protected void freeResourcesInner(HostRemoter remoter,
                                      ValidationResultsHolder validation) throws
                                                                          Exception
    {
        DockerContainerRecord rec = getContainer();
        if (rec != null)
        {
            container = null;

            rec.deleteRecursively(remoter, validation);
        }
    }

    @Override
    protected List<? extends RecordWithResources> deleteRecursivelyInner(HostRemoter remoter,
                                                                         ValidationResultsHolder validation)
    {
        // Nothing to do.
        return null;
    }
}
