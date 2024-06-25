/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.worker;

import static java.util.Objects.requireNonNull;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.builder.HostRemoter;
import com.optio3.cloud.builder.persistence.jobs.JobRecord;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.logging.Logger;
import com.optio3.util.Exceptions;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Table(name = "RESOURCES_TRACKED")
@Optio3TableInfo(externalId = "ResourcesTracked", model = BaseModel.class, metamodel = TrackedRecordWithResources_.class)
public abstract class TrackedRecordWithResources extends RecordWithResources
{
    public static final Logger LoggerInstance = new Logger(TrackedRecordWithResources.class);

    /**
     * If set, the resource is used by the referenced job.
     */
    @Optio3ControlNotifications(reason = "Notify changes", direct = Notify.ALWAYS, reverse = Notify.ALWAYS, getter = "getAcquiredBy")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "acquired_by", foreignKey = @ForeignKey(name = "ACQUIRED_BY__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private JobRecord acquiredBy;

    /**
     * If set, the resource will be deleted when released from a job.
     */
    private boolean deleteOnRelease;

    //--//

    public JobRecord getAcquiredBy()
    {
        return acquiredBy;
    }

    public boolean isDeleteOnRelease()
    {
        return deleteOnRelease;
    }

    public void setDeleteOnRelease(boolean deleteOnRelease)
    {
        this.deleteOnRelease = deleteOnRelease;
    }

    public void acquire(JobRecord job)
    {
        requireNonNull(job);

        if (acquiredBy != job)
        {
            if (acquiredBy != null)
            {
                throw Exceptions.newRuntimeException("Attempt to acquire %s (already associated with %s) for Job %s",
                                                     getResourceDisplayName(),
                                                     JobRecord.getDisplayName(acquiredBy),
                                                     JobRecord.getDisplayName(job));
            }

            acquiredBy = job;
            LoggerInstance.debug("Resource %s acquired by job %s", getResourceDisplayName(), JobRecord.getDisplayName(job));
        }
    }

    protected void acquireChild(JobRecord job,
                                TrackedRecordWithResources child)
    {
        if (child != null)
        {
            child.acquire(job);
        }
    }

    public void release(HostRemoter remoter,
                        SessionHolder sessionHolder) throws
                                                     Exception
    {
        requireNonNull(remoter);
        requireNonNull(sessionHolder);

        // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
        if (acquiredBy != null)
        {
            LoggerInstance.debug("Resource %s released from job %s", getResourceDisplayName(), JobRecord.getDisplayName(acquiredBy));
            acquiredBy = null;
        }

        if (deleteOnRelease)
        {
            try (ValidationResultsHolder validation = new ValidationResultsHolder(sessionHolder, null, false))
            {
                deleteRecursively(remoter, validation);
            }
        }
    }

    protected void releaseChild(HostRemoter remoter,
                                SessionHolder sessionHolder,
                                TrackedRecordWithResources child) throws
                                                                  Exception
    {
        if (child != null)
        {
            child.release(remoter, sessionHolder);
        }
    }

    public String getResourceDisplayName()
    {
        return String.format("%s:%s", getClass().getSimpleName(), getSysId());
    }
}
