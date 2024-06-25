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
@StaticMetamodel(ManagedDirectoryRecord.class)
public abstract class ManagedDirectoryRecord_ extends com.optio3.cloud.builder.persistence.worker.HostBoundResource_
{

    public static volatile SingularAttribute<ManagedDirectoryRecord, String>               path;
    public static volatile ListAttribute<ManagedDirectoryRecord, RepositoryCheckoutRecord> checkoutsForWork;
    public static volatile ListAttribute<ManagedDirectoryRecord, RepositoryCheckoutRecord> checkoutsForDb;
    public static volatile ListAttribute<ManagedDirectoryRecord, MappedDockerVolumeRecord> mappedIn;

    public static final String PATH               = "path";
    public static final String CHECKOUTS_FOR_WORK = "checkoutsForWork";
    public static final String CHECKOUTS_FOR_DB   = "checkoutsForDb";
    public static final String MAPPED_IN          = "mappedIn";
}

