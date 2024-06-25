/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.builder.persistence.worker;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(MappedDockerVolumeRecord.class)
public abstract class MappedDockerVolumeRecord_ extends com.optio3.cloud.persistence.RecordWithCommonFields_
{

    public static volatile SingularAttribute<MappedDockerVolumeRecord, DockerVolumeRecord>     volume;
    public static volatile SingularAttribute<MappedDockerVolumeRecord, String>                 path;
    public static volatile SingularAttribute<MappedDockerVolumeRecord, ManagedDirectoryRecord> directory;
    public static volatile SingularAttribute<MappedDockerVolumeRecord, DockerContainerRecord>  owningContainer;

    public static final String VOLUME           = "volume";
    public static final String PATH             = "path";
    public static final String DIRECTORY        = "directory";
    public static final String OWNING_CONTAINER = "owningContainer";
}

