/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.builder.persistence.worker;

import javax.annotation.Generated;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(DockerVolumeRecord.class)
public abstract class DockerVolumeRecord_ extends com.optio3.cloud.builder.persistence.worker.HostBoundResource_
{

    public static volatile ListAttribute<DockerVolumeRecord, MappedDockerVolumeRecord> mappedAs;
    public static volatile SingularAttribute<DockerVolumeRecord, String>               dockerId;

    public static final String MAPPED_AS = "mappedAs";
    public static final String DOCKER_ID = "dockerId";
}

