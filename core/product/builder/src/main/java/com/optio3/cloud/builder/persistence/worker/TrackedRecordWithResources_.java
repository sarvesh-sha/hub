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

import com.optio3.cloud.builder.persistence.jobs.JobRecord;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(TrackedRecordWithResources.class)
public abstract class TrackedRecordWithResources_ extends com.optio3.cloud.builder.persistence.worker.RecordWithResources_
{

    public static volatile SingularAttribute<TrackedRecordWithResources, Boolean>   deleteOnRelease;
    public static volatile SingularAttribute<TrackedRecordWithResources, JobRecord> acquiredBy;

    public static final String DELETE_ON_RELEASE = "deleteOnRelease";
    public static final String ACQUIRED_BY       = "acquiredBy";
}

