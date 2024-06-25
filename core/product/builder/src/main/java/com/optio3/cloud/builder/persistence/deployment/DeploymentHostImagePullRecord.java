/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.deployment;

import static java.util.Objects.requireNonNull;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
import javax.persistence.criteria.Predicate;

import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3Cascade;
import com.optio3.cloud.annotation.Optio3Cascade.Flavor;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.builder.model.deployment.DeploymentHostImagePull;
import com.optio3.cloud.builder.model.deployment.DeploymentHostImagePullFilterRequest;
import com.optio3.cloud.builder.model.jobs.JobStatus;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryTaggedImageRecord;
import com.optio3.cloud.model.SortCriteria;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.LogHandler;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.RecordWithCommonFields_;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.util.CollectionUtils;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Table(name = "DEPLOYMENT_HOST_IMAGE_PULL", indexes = { @Index(name = "DEPLOYMENT_HOST_IMAGE_PULL__CREATED", columnList = "sys_created_on") })
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "DeploymentHostImagePull", model = DeploymentHostImagePull.class, metamodel = DeploymentHostImagePullRecord_.class)
public class DeploymentHostImagePullRecord extends RecordWithCommonFields implements ModelMapperTarget<DeploymentHostImagePull, DeploymentHostImagePullRecord_>,
                                                                                     LogHandler.ILogHost<DeploymentHostImagePullLogRecord>
{
    /**
     * The deployment this image pull is for.
     */
    @Optio3ControlNotifications(reason = "Only notify host of image pull's changes", direct = Notify.ALWAYS, reverse = Notify.NEVER, getter = "getDeployment")
    @Optio3Cascade(mode = Flavor.DELETE, getter = "getDeployment")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "deployment", nullable = false, foreignKey = @ForeignKey(name = "IMAGE_PULL__DEPLOYMENT__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private DeploymentHostRecord deployment;

    //--//

    @Optio3ControlNotifications(reason = "Don't notify image", direct = Notify.ON_ASSOCIATION_CHANGES, reverse = Notify.NEVER, getter = "getImageReference")
    @Optio3Cascade(mode = Flavor.DELETE, getter = "getImageReference")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "image_reference", foreignKey = @ForeignKey(name = "IMAGE_PULL__IMAGE_REFERENCE__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private RegistryTaggedImageRecord imageReference;

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

    public DeploymentHostImagePullRecord()
    {
    }

    public static DeploymentHostImagePullRecord newInstance(DeploymentHostRecord host,
                                                            RegistryTaggedImageRecord image)
    {
        requireNonNull(host);

        DeploymentHostImagePullRecord res = new DeploymentHostImagePullRecord();
        res.deployment     = host;
        res.imageReference = image;
        res.status         = JobStatus.EXECUTING;
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
    public void refineLogQuery(LogHandler.JoinHelper<?, DeploymentHostImagePullLogRecord> jh)
    {
        jh.addWhereClauseWithEqual(jh.rootLog, DeploymentHostImagePullLogRecord_.owningImagePull, this);
    }

    @Override
    public DeploymentHostImagePullLogRecord allocateNewLogInstance()
    {
        return DeploymentHostImagePullLogRecord.newInstance(this);
    }

    public static LogHandler<DeploymentHostImagePullRecord, DeploymentHostImagePullLogRecord> allocateLogHandler(RecordLocked<DeploymentHostImagePullRecord> lock)
    {
        return new LogHandler<>(lock, DeploymentHostImagePullLogRecord.class);
    }

    public static LogHandler<DeploymentHostImagePullRecord, DeploymentHostImagePullLogRecord> allocateLogHandler(SessionHolder sessionHolder,
                                                                                                                 DeploymentHostImagePullRecord rec)
    {
        return new LogHandler<>(sessionHolder, rec, DeploymentHostImagePullLogRecord.class);
    }

    //--//

    public DeploymentHostRecord getDeployment()
    {
        return deployment;
    }

    public String getImage()
    {
        return imageReference.getTag();
    }

    public RegistryTaggedImageRecord getImageReference()
    {
        return imageReference;
    }

    public JobStatus getStatus()
    {
        return status;
    }

    public void setStatus(JobStatus status)
    {
        this.status = status;
    }

    //--//

    public static TypedRecordIdentityList<DeploymentHostImagePullRecord> filterPulls(RecordHelper<DeploymentHostImagePullRecord> helper,
                                                                                     DeploymentHostImagePullFilterRequest filtersIn)
    {
        DeploymentHostImagePullFilterRequest filters = Objects.requireNonNullElseGet(filtersIn, DeploymentHostImagePullFilterRequest::new);

        if (CollectionUtils.isEmpty(filters.sortBy))
        {
            SortCriteria sc = new SortCriteria();
            sc.column = "createdOn";

            filters.sortBy = Lists.newArrayList();
            filters.sortBy.add(sc);
        }

        return QueryHelperWithCommonFields.list(helper, (jh) ->
        {
            for (SortCriteria sort : filters.sortBy)
            {
                switch (sort.column)
                {
                    case "createdOn":
                    {
                        jh.addOrderBy(jh.root, RecordWithCommonFields_.createdOn, sort.ascending);

                        jh.addOrderBy(jh.root, RecordWithCommonFields_.createdOn, false);
                        break;
                    }

                    case "updatedOn":
                    {
                        jh.addOrderBy(jh.root, RecordWithCommonFields_.updatedOn, sort.ascending);
                        break;
                    }
                }
            }

            if (filters.hostSysId != null)
            {
                RecordHelper<DeploymentHostRecord> helper2  = helper.wrapFor(DeploymentHostRecord.class);
                DeploymentHostRecord               rec_host = helper2.getOrNull(filters.hostSysId);

                jh.addWhereClauseWithEqual(jh.root, DeploymentHostImagePullRecord_.deployment, rec_host);
            }

            if (filters.newerThan != null)
            {
                jh.addWhereClause(jh.greaterThanOrEqualTo(jh.root, RecordWithCommonFields_.createdOn, filters.newerThan));
            }

            if (filters.olderThan != null)
            {
                jh.addWhereClause(jh.lessThanOrEqualTo(jh.root, RecordWithCommonFields_.createdOn, filters.olderThan));
            }

            if (filters.statusFilter != null)
            {
                List<JobStatus> statusList = Lists.newArrayList(filters.statusFilter);
                if (filters.statusFilter == JobStatus.FAILED)
                {
                    statusList.add(JobStatus.TIMEOUT);
                    statusList.add(JobStatus.UNKNOWNTOKEN);
                }

                Predicate[] preds = new Predicate[statusList.size()];
                int         pos   = 0;
                for (JobStatus status : statusList)
                {
                    preds[pos++] = jh.equal(jh.root, DeploymentHostImagePullRecord_.status, status);
                }

                jh.addWhereClause(jh.or(preds));
            }
        });
    }

    public static List<DeploymentHostImagePullRecord> getBatch(RecordHelper<DeploymentHostImagePullRecord> helper,
                                                               List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }

    public void remove(ValidationResultsHolder validation,
                       RecordHelper<DeploymentHostImagePullRecord> agentHelper)
    {
        if (validation.canProceed())
        {
            agentHelper.delete(this);
        }
    }
}
