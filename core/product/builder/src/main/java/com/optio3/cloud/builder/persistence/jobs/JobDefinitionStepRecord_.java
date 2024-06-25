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
@StaticMetamodel(JobDefinitionStepRecord.class)
public abstract class JobDefinitionStepRecord_ extends com.optio3.cloud.persistence.RecordWithMetadata_
{

    public static volatile SingularAttribute<JobDefinitionStepRecord, JobDefinitionRecord> owningJob;
    public static volatile SingularAttribute<JobDefinitionStepRecord, String>              name;
    public static volatile SingularAttribute<JobDefinitionStepRecord, String>              buildId;
    public static volatile SingularAttribute<JobDefinitionStepRecord, Integer>             position;
    public static volatile SingularAttribute<JobDefinitionStepRecord, Integer>             timeout;

    public static final String OWNING_JOB = "owningJob";
    public static final String NAME       = "name";
    public static final String BUILD_ID   = "buildId";
    public static final String POSITION   = "position";
    public static final String TIMEOUT    = "timeout";
}

