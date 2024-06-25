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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.optio3.cloud.annotation.Optio3Cascade;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.builder.model.jobs.JobSource;
import com.optio3.cloud.builder.persistence.jobs.input.RepositoryRecord;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Table(name = "JOB_SOURCE")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "JobSource", model = JobSource.class, metamodel = JobSourceRecord_.class)
public class JobSourceRecord extends RecordWithCommonFields implements ModelMapperTarget<JobSource, JobSourceRecord_>
{
    /**
     * Bound to this job.
     */
    @Optio3ControlNotifications(reason = "Ignore changes", direct = Notify.NEVER, reverse = Notify.NEVER)
    @Optio3Cascade(mode = Optio3Cascade.Flavor.DELETE, getter = "getOwningJob")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "owning_job", nullable = false, foreignKey = @ForeignKey(name = "JOBSOURCE__OWNING_JOB__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private JobRecord owningJob;

    @Optio3ControlNotifications(reason = "Ignore changes", direct = Notify.NEVER, reverse = Notify.NEVER)
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "repo", nullable = false, foreignKey = @ForeignKey(name = "JOBSOURCE__REPO__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private RepositoryRecord repo;

    @Column(name = "branch", nullable = false)
    private String branch;

    @Column(name = "commit", nullable = false)
    private String commit;

    //--//

    public JobSourceRecord()
    {
    }

    public static JobSourceRecord newInstance(JobRecord job,
                                              RepositoryRecord repo,
                                              String branch,
                                              String commit)
    {
        requireNonNull(job);

        JobSourceRecord res = new JobSourceRecord();
        res.owningJob = job;
        res.repo      = repo;
        res.branch    = branch;
        res.commit    = commit;
        return res;
    }

    //--//

    public JobRecord getOwningJob()
    {
        return owningJob;
    }

    public RepositoryRecord getRepo()
    {
        return repo;
    }

    public String getBranch()
    {
        return branch;
    }

    public String getCommit()
    {
        return commit;
    }

    //--//

    public static List<JobSourceRecord> getBatch(RecordHelper<JobSourceRecord> helper,
                                                 List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }
}
