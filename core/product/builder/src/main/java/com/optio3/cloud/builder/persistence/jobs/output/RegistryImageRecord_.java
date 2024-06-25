/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.builder.persistence.jobs.output;

import java.time.ZonedDateTime;

import javax.annotation.Generated;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.builder.persistence.deployment.DeploymentTaskRecord;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(RegistryImageRecord.class)
public abstract class RegistryImageRecord_ extends com.optio3.cloud.persistence.RecordWithMetadata_
{

    public static volatile SingularAttribute<RegistryImageRecord, String>                  imageSha;
    public static volatile SingularAttribute<RegistryImageRecord, DeploymentRole>          targetService;
    public static volatile SingularAttribute<RegistryImageRecord, ZonedDateTime>           buildTime;
    public static volatile ListAttribute<RegistryImageRecord, RegistryTaggedImageRecord>   referencingTags;
    public static volatile ListAttribute<RegistryImageRecord, DeploymentTaskRecord>        referencingTasks;
    public static volatile SingularAttribute<RegistryImageRecord, DockerImageArchitecture> architecture;

    public static final String IMAGE_SHA         = "imageSha";
    public static final String TARGET_SERVICE    = "targetService";
    public static final String BUILD_TIME        = "buildTime";
    public static final String REFERENCING_TAGS  = "referencingTags";
    public static final String REFERENCING_TASKS = "referencingTasks";
    public static final String ARCHITECTURE      = "architecture";
}

