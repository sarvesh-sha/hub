/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.builder.persistence.deployment;

import java.time.ZonedDateTime;

import javax.annotation.Generated;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

import com.optio3.cloud.builder.model.deployment.DeploymentOperationalStatus;
import com.optio3.cloud.builder.model.deployment.DeploymentStatus;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(DeploymentHostRecord.class)
public abstract class DeploymentHostRecord_ extends com.optio3.cloud.persistence.RecordWithMetadata_
{

    public static volatile SingularAttribute<DeploymentHostRecord, DeploymentOperationalStatus> operationalStatus;
    public static volatile SingularAttribute<DeploymentHostRecord, byte[]>                      logRanges;
    public static volatile SingularAttribute<DeploymentHostRecord, String>                      hostName;
    public static volatile SingularAttribute<DeploymentHostRecord, ZonedDateTime>               lastHeartbeat;
    public static volatile SingularAttribute<DeploymentHostRecord, String>                      hostId;
    public static volatile SingularAttribute<DeploymentHostRecord, String>                      dnsName;
    public static volatile SingularAttribute<DeploymentHostRecord, Integer>                     lastOffset;
    public static volatile SingularAttribute<DeploymentHostRecord, Integer>                     warningThreshold;
    public static volatile SingularAttribute<DeploymentHostRecord, ZonedDateTime>               lastOutput;
    public static volatile ListAttribute<DeploymentHostRecord, DeploymentAgentRecord>           agents;
    public static volatile SingularAttribute<DeploymentHostRecord, Long>                        roleIds;
    public static volatile SingularAttribute<DeploymentHostRecord, CustomerServiceRecord>       customerService;
    public static volatile SingularAttribute<DeploymentHostRecord, Boolean>                     hasDelayedOps;
    public static volatile ListAttribute<DeploymentHostRecord, DeploymentHostFileRecord>        files;
    public static volatile SingularAttribute<DeploymentHostRecord, String>                      details;
    public static volatile ListAttribute<DeploymentHostRecord, DeploymentHostImagePullRecord>   imagePulls;
    public static volatile ListAttribute<DeploymentHostRecord, DeploymentTaskRecord>            tasks;
    public static volatile SingularAttribute<DeploymentHostRecord, DeploymentStatus>            status;
    public static volatile SingularAttribute<DeploymentHostRecord, DockerImageArchitecture>     architecture;

    public static final String OPERATIONAL_STATUS = "operationalStatus";
    public static final String LOG_RANGES         = "logRanges";
    public static final String HOST_NAME          = "hostName";
    public static final String LAST_HEARTBEAT     = "lastHeartbeat";
    public static final String HOST_ID            = "hostId";
    public static final String DNS_NAME           = "dnsName";
    public static final String LAST_OFFSET        = "lastOffset";
    public static final String WARNING_THRESHOLD  = "warningThreshold";
    public static final String LAST_OUTPUT        = "lastOutput";
    public static final String AGENTS             = "agents";
    public static final String ROLE_IDS           = "roleIds";
    public static final String CUSTOMER_SERVICE   = "customerService";
    public static final String HAS_DELAYED_OPS    = "hasDelayedOps";
    public static final String FILES              = "files";
    public static final String DETAILS            = "details";
    public static final String IMAGE_PULLS        = "imagePulls";
    public static final String TASKS              = "tasks";
    public static final String STATUS             = "status";
    public static final String ARCHITECTURE       = "architecture";
}

