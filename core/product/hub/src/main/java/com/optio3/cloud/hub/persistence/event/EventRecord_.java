/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.hub.persistence.event;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.location.LocationRecord;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(EventRecord.class)
public abstract class EventRecord_ extends com.optio3.cloud.persistence.RecordWithSequenceNumber_
{

    public static volatile SingularAttribute<EventRecord, Integer>        sequenceNumber;
    public static volatile SingularAttribute<EventRecord, String>         description;
    public static volatile SingularAttribute<EventRecord, LocationRecord> location;
    public static volatile SingularAttribute<EventRecord, AssetRecord>    asset;
    public static volatile SingularAttribute<EventRecord, String>         extendedDescription;

    public static final String SEQUENCE_NUMBER      = "sequenceNumber";
    public static final String DESCRIPTION          = "description";
    public static final String LOCATION             = "location";
    public static final String ASSET                = "asset";
    public static final String EXTENDED_DESCRIPTION = "extendedDescription";
}

