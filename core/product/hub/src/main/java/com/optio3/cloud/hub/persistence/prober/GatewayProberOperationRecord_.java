/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.hub.persistence.prober;

import java.time.ZonedDateTime;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.hub.persistence.asset.GatewayAssetRecord;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(GatewayProberOperationRecord.class)
public abstract class GatewayProberOperationRecord_ extends com.optio3.cloud.persistence.RecordWithHeartbeat_
{

    public static volatile SingularAttribute<GatewayProberOperationRecord, byte[]>                   logRanges;
    public static volatile SingularAttribute<GatewayProberOperationRecord, String>                   outputDetails;
    public static volatile SingularAttribute<GatewayProberOperationRecord, BackgroundActivityRecord> currentActivity;
    public static volatile SingularAttribute<GatewayProberOperationRecord, Integer>                  lastOffset;
    public static volatile SingularAttribute<GatewayProberOperationRecord, String>                   inputDetails;
    public static volatile SingularAttribute<GatewayProberOperationRecord, GatewayAssetRecord>       gateway;
    public static volatile SingularAttribute<GatewayProberOperationRecord, ZonedDateTime>            lastOutput;

    public static final String LOG_RANGES       = "logRanges";
    public static final String OUTPUT_DETAILS   = "outputDetails";
    public static final String CURRENT_ACTIVITY = "currentActivity";
    public static final String LAST_OFFSET      = "lastOffset";
    public static final String INPUT_DETAILS    = "inputDetails";
    public static final String GATEWAY          = "gateway";
    public static final String LAST_OUTPUT      = "lastOutput";
}

