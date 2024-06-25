/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.hub.persistence.asset;

import java.time.ZonedDateTime;

import javax.annotation.Generated;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(NetworkAssetRecord.class)
public abstract class NetworkAssetRecord_ extends com.optio3.cloud.hub.persistence.asset.AssetRecord_
{

    public static volatile SingularAttribute<NetworkAssetRecord, String>        discoveryState;
    public static volatile SingularAttribute<NetworkAssetRecord, byte[]>        logRanges;
    public static volatile SingularAttribute<NetworkAssetRecord, String>        networkInterface;
    public static volatile SingularAttribute<NetworkAssetRecord, String>        staticAddress;
    public static volatile SingularAttribute<NetworkAssetRecord, Integer>       samplingPeriod;
    public static volatile SingularAttribute<NetworkAssetRecord, Integer>       lastOffset;
    public static volatile SingularAttribute<NetworkAssetRecord, String>        cidr;
    public static volatile SetAttribute<NetworkAssetRecord, GatewayAssetRecord> boundGateways;
    public static volatile SingularAttribute<NetworkAssetRecord, String>        protocolsConfiguration;
    public static volatile SingularAttribute<NetworkAssetRecord, ZonedDateTime> lastOutput;

    public static final String DISCOVERY_STATE         = "discoveryState";
    public static final String LOG_RANGES              = "logRanges";
    public static final String NETWORK_INTERFACE       = "networkInterface";
    public static final String STATIC_ADDRESS          = "staticAddress";
    public static final String SAMPLING_PERIOD         = "samplingPeriod";
    public static final String LAST_OFFSET             = "lastOffset";
    public static final String CIDR                    = "cidr";
    public static final String BOUND_GATEWAYS          = "boundGateways";
    public static final String PROTOCOLS_CONFIGURATION = "protocolsConfiguration";
    public static final String LAST_OUTPUT             = "lastOutput";
}

