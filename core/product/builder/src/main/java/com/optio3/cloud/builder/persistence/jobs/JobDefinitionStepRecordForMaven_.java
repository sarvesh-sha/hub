/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.builder.persistence.jobs;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(JobDefinitionStepRecordForMaven.class)
public abstract class JobDefinitionStepRecordForMaven_ extends com.optio3.cloud.builder.persistence.jobs.JobDefinitionStepRecord_
{

    public static volatile SingularAttribute<JobDefinitionStepRecordForMaven, String> pullFrom;
    public static volatile SingularAttribute<JobDefinitionStepRecordForMaven, String> directory;

    public static final String PULL_FROM = "pullFrom";
    public static final String DIRECTORY = "directory";
}

