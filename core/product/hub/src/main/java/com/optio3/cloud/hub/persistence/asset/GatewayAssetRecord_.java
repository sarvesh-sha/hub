/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.hub.persistence.asset;

import java.time.ZonedDateTime;

import javax.annotation.Generated;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

import com.optio3.cloud.hub.persistence.prober.GatewayProberOperationRecord;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(GatewayAssetRecord.class)
public abstract class GatewayAssetRecord_ extends com.optio3.cloud.hub.persistence.asset.AssetRecord_
{

    public static volatile ListAttribute<GatewayAssetRecord, NetworkAssetRecord>           boundNetworks;
    public static volatile SingularAttribute<GatewayAssetRecord, byte[]>                   logRanges;
    public static volatile SingularAttribute<GatewayAssetRecord, Integer>                  cpuLoad;
    public static volatile SingularAttribute<GatewayAssetRecord, String>                   instanceId;
    public static volatile ListAttribute<GatewayAssetRecord, GatewayProberOperationRecord> operations;
    public static volatile SingularAttribute<GatewayAssetRecord, Integer>                  alertThreshold;
    public static volatile SingularAttribute<GatewayAssetRecord, Integer>                  lastOffset;
    public static volatile SingularAttribute<GatewayAssetRecord, String>                   rpcId;
    public static volatile SingularAttribute<GatewayAssetRecord, String>                   details;
    public static volatile SingularAttribute<GatewayAssetRecord, Integer>                  warningThreshold;
    public static volatile SingularAttribute<GatewayAssetRecord, Integer>                  cpuLoadPrevious;
    public static volatile SingularAttribute<GatewayAssetRecord, ZonedDateTime>            lastOutput;

    public static final String BOUND_NETWORKS    = "boundNetworks";
    public static final String LOG_RANGES        = "logRanges";
    public static final String CPU_LOAD          = "cpuLoad";
    public static final String INSTANCE_ID       = "instanceId";
    public static final String OPERATIONS        = "operations";
    public static final String ALERT_THRESHOLD   = "alertThreshold";
    public static final String LAST_OFFSET       = "lastOffset";
    public static final String RPC_ID            = "rpcId";
    public static final String DETAILS           = "details";
    public static final String WARNING_THRESHOLD = "warningThreshold";
    public static final String CPU_LOAD_PREVIOUS = "cpuLoadPrevious";
    public static final String LAST_OUTPUT       = "lastOutput";
}

