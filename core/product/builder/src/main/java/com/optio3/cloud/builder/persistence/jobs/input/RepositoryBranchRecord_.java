/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.builder.persistence.jobs.input;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(RepositoryBranchRecord.class)
public abstract class RepositoryBranchRecord_ extends com.optio3.cloud.persistence.RecordWithCommonFields_
{

    public static volatile SingularAttribute<RepositoryBranchRecord, RepositoryCommitRecord> head;
    public static volatile SingularAttribute<RepositoryBranchRecord, String>                 name;
    public static volatile SingularAttribute<RepositoryBranchRecord, RepositoryRecord>       repository;

    public static final String HEAD       = "head";
    public static final String NAME       = "name";
    public static final String REPOSITORY = "repository";
}

