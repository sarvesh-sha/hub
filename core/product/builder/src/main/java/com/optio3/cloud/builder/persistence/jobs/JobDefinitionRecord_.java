/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.builder.persistence.jobs;

import javax.annotation.Generated;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(JobDefinitionRecord.class)
public abstract class JobDefinitionRecord_ extends com.optio3.cloud.persistence.RecordWithCommonFields_
{

    public static volatile SingularAttribute<JobDefinitionRecord, String>              name;
    public static volatile SingularAttribute<JobDefinitionRecord, String>              idPrefix;
    public static volatile SingularAttribute<JobDefinitionRecord, Integer>             totalTimeout;
    public static volatile ListAttribute<JobDefinitionRecord, JobDefinitionStepRecord> steps;

    public static final String NAME          = "name";
    public static final String ID_PREFIX     = "idPrefix";
    public static final String TOTAL_TIMEOUT = "totalTimeout";
    public static final String STEPS         = "steps";
}

