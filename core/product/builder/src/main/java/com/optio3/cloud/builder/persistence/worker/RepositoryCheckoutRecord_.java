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

import com.optio3.cloud.builder.persistence.jobs.input.RepositoryRecord;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(RepositoryCheckoutRecord.class)
public abstract class RepositoryCheckoutRecord_ extends com.optio3.cloud.builder.persistence.worker.HostBoundResource_
{

    public static volatile SingularAttribute<RepositoryCheckoutRecord, String>                 currentCommit;
    public static volatile SingularAttribute<RepositoryCheckoutRecord, String>                 currentBranch;
    public static volatile SingularAttribute<RepositoryCheckoutRecord, ManagedDirectoryRecord> directoryForDb;
    public static volatile SingularAttribute<RepositoryCheckoutRecord, RepositoryRecord>       repository;
    public static volatile SingularAttribute<RepositoryCheckoutRecord, ManagedDirectoryRecord> directoryForWork;

    public static final String CURRENT_COMMIT     = "currentCommit";
    public static final String CURRENT_BRANCH     = "currentBranch";
    public static final String DIRECTORY_FOR_DB   = "directoryForDb";
    public static final String REPOSITORY         = "repository";
    public static final String DIRECTORY_FOR_WORK = "directoryForWork";
}

