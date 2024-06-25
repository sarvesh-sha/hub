/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.builder.persistence.customer;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(RoleAndArchitecture.class)
public abstract class RoleAndArchitecture_
{

    public static volatile SingularAttribute<RoleAndArchitecture, DeploymentRole>          role;
    public static volatile SingularAttribute<RoleAndArchitecture, DockerImageArchitecture> architecture;

    public static final String ROLE         = "role";
    public static final String ARCHITECTURE = "architecture";
}

