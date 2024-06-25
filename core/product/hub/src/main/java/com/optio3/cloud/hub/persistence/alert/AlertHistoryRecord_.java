/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.hub.persistence.alert;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

import com.optio3.cloud.hub.model.alert.AlertEventLevel;
import com.optio3.cloud.hub.model.alert.AlertEventType;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(AlertHistoryRecord.class)
public abstract class AlertHistoryRecord_ extends com.optio3.cloud.persistence.RecordWithCommonFields_
{

    public static volatile SingularAttribute<AlertHistoryRecord, AlertRecord>     alert;
    public static volatile SingularAttribute<AlertHistoryRecord, AlertEventLevel> level;
    public static volatile SingularAttribute<AlertHistoryRecord, String>          text;
    public static volatile SingularAttribute<AlertHistoryRecord, AlertEventType>  type;

    public static final String ALERT = "alert";
    public static final String LEVEL = "level";
    public static final String TEXT  = "text";
    public static final String TYPE  = "type";
}

