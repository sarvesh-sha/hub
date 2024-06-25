/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.builder.persistence.deployment;

import java.time.ZonedDateTime;

import javax.annotation.Generated;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(DeploymentHostFileRecord.class)
public abstract class DeploymentHostFileRecord_ extends com.optio3.cloud.persistence.RecordWithCommonFields_
{

    public static volatile SingularAttribute<DeploymentHostFileRecord, String>                    path;
    public static volatile SingularAttribute<DeploymentHostFileRecord, DeploymentTaskRecord>      task;
    public static volatile SingularAttribute<DeploymentHostFileRecord, ZonedDateTime>             downloadedOn;
    public static volatile SingularAttribute<DeploymentHostFileRecord, ZonedDateTime>             uploadedOn;
    public static volatile ListAttribute<DeploymentHostFileRecord, DeploymentHostFileChunkRecord> chunks;
    public static volatile SingularAttribute<DeploymentHostFileRecord, String>                    taskName;
    public static volatile SingularAttribute<DeploymentHostFileRecord, DeploymentHostRecord>      deployment;

    public static final String PATH          = "path";
    public static final String TASK          = "task";
    public static final String DOWNLOADED_ON = "downloadedOn";
    public static final String UPLOADED_ON   = "uploadedOn";
    public static final String CHUNKS        = "chunks";
    public static final String TASK_NAME     = "taskName";
    public static final String DEPLOYMENT    = "deployment";
}

