/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.builder.persistence.jobs;

import java.time.ZonedDateTime;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

import com.optio3.cloud.builder.model.jobs.JobStatus;
import com.optio3.cloud.builder.persistence.worker.DockerContainerRecord;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(JobStepRecord.class)
public abstract class JobStepRecord_ extends com.optio3.cloud.builder.persistence.worker.RecordWithResources_
{

    public static volatile SingularAttribute<JobStepRecord, byte[]>                logRanges;
    public static volatile SingularAttribute<JobStepRecord, DockerContainerRecord> container;
    public static volatile SingularAttribute<JobStepRecord, JobRecord>             owningJob;
    public static volatile SingularAttribute<JobStepRecord, String>                name;
    public static volatile SingularAttribute<JobStepRecord, Integer>               lastOffset;
    public static volatile SingularAttribute<JobStepRecord, ZonedDateTime>         lastOutput;
    public static volatile SingularAttribute<JobStepRecord, JobStatus>             status;

    public static final String LOG_RANGES  = "logRanges";
    public static final String CONTAINER   = "container";
    public static final String OWNING_JOB  = "owningJob";
    public static final String NAME        = "name";
    public static final String LAST_OFFSET = "lastOffset";
    public static final String LAST_OUTPUT = "lastOutput";
    public static final String STATUS      = "status";
}

