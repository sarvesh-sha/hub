/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.persistence;

import java.time.Duration;
import java.time.ZonedDateTime;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

import com.optio3.cloud.logic.BackgroundActivityStatus;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(RecordForBackgroundActivity.class)
public abstract class RecordForBackgroundActivity_ extends com.optio3.cloud.persistence.RecordWithCommonFields_
{

    public static volatile SingularAttribute<RecordForBackgroundActivity, String>                   handler;
    public static volatile SingularAttribute<RecordForBackgroundActivity, BackgroundActivityStatus> lastActivationStatus;
    public static volatile SingularAttribute<RecordForBackgroundActivity, ZonedDateTime>            nextActivation;
    public static volatile SingularAttribute<RecordForBackgroundActivity, String>                   title;
    public static volatile SingularAttribute<RecordForBackgroundActivity, Boolean>                  wasProcessing;
    public static volatile SingularAttribute<RecordForBackgroundActivity, ZonedDateTime>            lastActivation;
    public static volatile SingularAttribute<RecordForBackgroundActivity, Duration>                 timeout;
    public static volatile SingularAttribute<RecordForBackgroundActivity, String>                   handlerKey;
    public static volatile SingularAttribute<RecordForBackgroundActivity, String>                   rpcId;
    public static volatile SingularAttribute<RecordForBackgroundActivity, String>                   lastActivationFailureTrace;
    public static volatile SingularAttribute<RecordForBackgroundActivity, byte[]>                   binaryHandlerState;
    public static volatile SingularAttribute<RecordForBackgroundActivity, String>                   lastActivationFailure;
    public static volatile SingularAttribute<RecordForBackgroundActivity, BackgroundActivityStatus> status;

    public static final String HANDLER                       = "handler";
    public static final String LAST_ACTIVATION_STATUS        = "lastActivationStatus";
    public static final String NEXT_ACTIVATION               = "nextActivation";
    public static final String TITLE                         = "title";
    public static final String WAS_PROCESSING                = "wasProcessing";
    public static final String LAST_ACTIVATION               = "lastActivation";
    public static final String TIMEOUT                       = "timeout";
    public static final String HANDLER_KEY                   = "handlerKey";
    public static final String RPC_ID                        = "rpcId";
    public static final String LAST_ACTIVATION_FAILURE_TRACE = "lastActivationFailureTrace";
    public static final String BINARY_HANDLER_STATE          = "binaryHandlerState";
    public static final String LAST_ACTIVATION_FAILURE       = "lastActivationFailure";
    public static final String STATUS                        = "status";
}

