/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.builder.persistence.deployment;

import java.time.ZonedDateTime;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

import com.optio3.cloud.builder.model.jobs.JobStatus;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryTaggedImageRecord;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(DeploymentHostImagePullRecord.class)
public abstract class DeploymentHostImagePullRecord_ extends com.optio3.cloud.persistence.RecordWithCommonFields_
{

    public static volatile SingularAttribute<DeploymentHostImagePullRecord, byte[]>                    logRanges;
    public static volatile SingularAttribute<DeploymentHostImagePullRecord, Integer>                   lastOffset;
    public static volatile SingularAttribute<DeploymentHostImagePullRecord, RegistryTaggedImageRecord> imageReference;
    public static volatile SingularAttribute<DeploymentHostImagePullRecord, ZonedDateTime>             lastOutput;
    public static volatile SingularAttribute<DeploymentHostImagePullRecord, DeploymentHostRecord>      deployment;
    public static volatile SingularAttribute<DeploymentHostImagePullRecord, JobStatus>                 status;

    public static final String LOG_RANGES      = "logRanges";
    public static final String LAST_OFFSET     = "lastOffset";
    public static final String IMAGE_REFERENCE = "imageReference";
    public static final String LAST_OUTPUT     = "lastOutput";
    public static final String DEPLOYMENT      = "deployment";
    public static final String STATUS          = "status";
}

