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

import com.optio3.cloud.builder.model.deployment.DeploymentStatus;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryImageRecord;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(DeploymentTaskRecord.class)
public abstract class DeploymentTaskRecord_ extends com.optio3.cloud.persistence.RecordWithHeartbeat_
{

    public static volatile SingularAttribute<DeploymentTaskRecord, byte[]>               logRanges;
    public static volatile SingularAttribute<DeploymentTaskRecord, String>               image;
    public static volatile SingularAttribute<DeploymentTaskRecord, DeploymentRole>       purpose;
    public static volatile SingularAttribute<DeploymentTaskRecord, String>               dockerId;
    public static volatile SingularAttribute<DeploymentTaskRecord, String>               name;
    public static volatile SingularAttribute<DeploymentTaskRecord, Integer>              lastOffset;
    public static volatile SingularAttribute<DeploymentTaskRecord, RegistryImageRecord>  imageReference;
    public static volatile SingularAttribute<DeploymentTaskRecord, ZonedDateTime>        lastOutput;
    public static volatile SingularAttribute<DeploymentTaskRecord, DeploymentHostRecord> deployment;
    public static volatile SingularAttribute<DeploymentTaskRecord, DeploymentStatus>     status;

    public static final String LOG_RANGES      = "logRanges";
    public static final String IMAGE           = "image";
    public static final String PURPOSE         = "purpose";
    public static final String DOCKER_ID       = "dockerId";
    public static final String NAME            = "name";
    public static final String LAST_OFFSET     = "lastOffset";
    public static final String IMAGE_REFERENCE = "imageReference";
    public static final String LAST_OUTPUT     = "lastOutput";
    public static final String DEPLOYMENT      = "deployment";
    public static final String STATUS          = "status";
}

