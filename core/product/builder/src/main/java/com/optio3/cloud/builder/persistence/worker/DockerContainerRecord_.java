/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.builder.persistence.worker;

import java.time.ZonedDateTime;

import javax.annotation.Generated;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(DockerContainerRecord.class)
public abstract class DockerContainerRecord_ extends com.optio3.cloud.builder.persistence.worker.HostBoundResource_
{

    public static volatile SingularAttribute<DockerContainerRecord, String>               dockerId;
    public static volatile SingularAttribute<DockerContainerRecord, Integer>              exitCode;
    public static volatile ListAttribute<DockerContainerRecord, MappedDockerVolumeRecord> mappedTo;
    public static volatile SingularAttribute<DockerContainerRecord, ZonedDateTime>        startedOn;

    public static final String DOCKER_ID  = "dockerId";
    public static final String EXIT_CODE  = "exitCode";
    public static final String MAPPED_TO  = "mappedTo";
    public static final String STARTED_ON = "startedOn";
}

