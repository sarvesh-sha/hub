/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.builder.persistence;

import javax.annotation.Generated;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

import com.optio3.cloud.builder.persistence.worker.HostRecord;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(BackgroundActivityRecord.class)
public abstract class BackgroundActivityRecord_ extends com.optio3.cloud.persistence.RecordForBackgroundActivity_
{

    public static volatile SetAttribute<BackgroundActivityRecord, BackgroundActivityRecord> waitingActivities;
    public static volatile SingularAttribute<BackgroundActivityRecord, HostRecord>          hostAffinity;
    public static volatile SetAttribute<BackgroundActivityRecord, BackgroundActivityRecord> subActivities;

    public static final String WAITING_ACTIVITIES = "waitingActivities";
    public static final String HOST_AFFINITY      = "hostAffinity";
    public static final String SUB_ACTIVITIES     = "subActivities";
}

