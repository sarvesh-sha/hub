/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.builder.persistence.deployment;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

import com.optio3.cloud.builder.model.deployment.DeploymentStatus;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(DeploymentAgentRecord.class)
public abstract class DeploymentAgentRecord_ extends com.optio3.cloud.persistence.RecordWithHeartbeat_
{

    public static volatile SingularAttribute<DeploymentAgentRecord, String>               instanceId;
    public static volatile SingularAttribute<DeploymentAgentRecord, String>               dockerId;
    public static volatile SingularAttribute<DeploymentAgentRecord, Boolean>              active;
    public static volatile SingularAttribute<DeploymentAgentRecord, String>               rpcId;
    public static volatile SingularAttribute<DeploymentAgentRecord, String>               details;
    public static volatile SingularAttribute<DeploymentAgentRecord, DeploymentHostRecord> deployment;
    public static volatile SingularAttribute<DeploymentAgentRecord, DeploymentStatus>     status;

    public static final String INSTANCE_ID = "instanceId";
    public static final String DOCKER_ID   = "dockerId";
    public static final String ACTIVE      = "active";
    public static final String RPC_ID      = "rpcId";
    public static final String DETAILS     = "details";
    public static final String DEPLOYMENT  = "deployment";
    public static final String STATUS      = "status";
}

