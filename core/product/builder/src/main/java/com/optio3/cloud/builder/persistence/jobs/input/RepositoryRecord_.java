/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.builder.persistence.jobs.input;

import javax.annotation.Generated;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

import com.optio3.cloud.builder.persistence.worker.RepositoryCheckoutRecord;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(RepositoryRecord.class)
public abstract class RepositoryRecord_ extends com.optio3.cloud.persistence.RecordWithCommonFields_
{

    public static volatile ListAttribute<RepositoryRecord, RepositoryCheckoutRecord> checkouts;
    public static volatile SingularAttribute<RepositoryRecord, String>               name;
    public static volatile ListAttribute<RepositoryRecord, RepositoryBranchRecord>   branches;
    public static volatile SingularAttribute<RepositoryRecord, String>               gitUrl;

    public static final String CHECKOUTS = "checkouts";
    public static final String NAME      = "name";
    public static final String BRANCHES  = "branches";
    public static final String GIT_URL   = "gitUrl";
}

