/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.hub.persistence;

import java.time.ZonedDateTime;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(HostAssetRecord.class)
public abstract class HostAssetRecord_ extends com.optio3.cloud.hub.persistence.asset.AssetRecord_
{

    public static volatile SingularAttribute<HostAssetRecord, byte[]>        logRanges;
    public static volatile SingularAttribute<HostAssetRecord, Integer>       lastOffset;
    public static volatile SingularAttribute<HostAssetRecord, ZonedDateTime> lastOutput;

    public static final String LOG_RANGES  = "logRanges";
    public static final String LAST_OFFSET = "lastOffset";
    public static final String LAST_OUTPUT = "lastOutput";
}

