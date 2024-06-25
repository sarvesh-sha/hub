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
@StaticMetamodel(JobDefinitionStepRecordForDockerRun.class)
public abstract class JobDefinitionStepRecordForDockerRun_ extends com.optio3.cloud.builder.persistence.jobs.JobDefinitionStepRecord_
{

    public static volatile SingularAttribute<JobDefinitionStepRecordForDockerRun, String>  image;
    public static volatile SingularAttribute<JobDefinitionStepRecordForDockerRun, Boolean> forcePull;
    public static volatile SingularAttribute<JobDefinitionStepRecordForDockerRun, String>  workingDirectory;
    public static volatile SingularAttribute<JobDefinitionStepRecordForDockerRun, String>  commandLine;

    public static final String IMAGE             = "image";
    public static final String FORCE_PULL        = "forcePull";
    public static final String WORKING_DIRECTORY = "workingDirectory";
    public static final String COMMAND_LINE      = "commandLine";
}

