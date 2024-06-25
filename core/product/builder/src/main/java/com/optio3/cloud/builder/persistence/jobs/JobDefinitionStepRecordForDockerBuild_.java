/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.builder.persistence.jobs;

import javax.annotation.Generated;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(JobDefinitionStepRecordForDockerBuild.class)
public abstract class JobDefinitionStepRecordForDockerBuild_ extends com.optio3.cloud.builder.persistence.jobs.JobDefinitionStepRecord_
{

    public static volatile SingularAttribute<JobDefinitionStepRecordForDockerBuild, String>                  dockerFile;
    public static volatile SingularAttribute<JobDefinitionStepRecordForDockerBuild, DeploymentRole>          targetService;
    public static volatile MapAttribute<JobDefinitionStepRecordForDockerBuild, String, String>               buildArgs;
    public static volatile SingularAttribute<JobDefinitionStepRecordForDockerBuild, String>                  sourcePath;
    public static volatile SingularAttribute<JobDefinitionStepRecordForDockerBuild, DockerImageArchitecture> architecture;

    public static final String DOCKER_FILE    = "dockerFile";
    public static final String TARGET_SERVICE = "targetService";
    public static final String BUILD_ARGS     = "buildArgs";
    public static final String SOURCE_PATH    = "sourcePath";
    public static final String ARCHITECTURE   = "architecture";
}

