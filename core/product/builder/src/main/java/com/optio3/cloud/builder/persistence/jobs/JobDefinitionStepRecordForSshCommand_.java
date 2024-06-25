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
@StaticMetamodel(JobDefinitionStepRecordForSshCommand.class)
public abstract class JobDefinitionStepRecordForSshCommand_ extends com.optio3.cloud.builder.persistence.jobs.JobDefinitionStepRecord_
{

    public static volatile SingularAttribute<JobDefinitionStepRecordForSshCommand, String> targetHost;
    public static volatile SingularAttribute<JobDefinitionStepRecordForSshCommand, String> credentials;
    public static volatile SingularAttribute<JobDefinitionStepRecordForSshCommand, String> commandLine;

    public static final String TARGET_HOST  = "targetHost";
    public static final String CREDENTIALS  = "credentials";
    public static final String COMMAND_LINE = "commandLine";
}

