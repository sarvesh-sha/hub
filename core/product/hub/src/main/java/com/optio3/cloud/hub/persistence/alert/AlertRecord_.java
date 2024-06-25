/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.hub.persistence.alert;

import javax.annotation.Generated;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

import com.optio3.cloud.hub.model.alert.AlertStatus;
import com.optio3.cloud.hub.model.alert.AlertType;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(AlertRecord.class)
public abstract class AlertRecord_ extends com.optio3.cloud.hub.persistence.event.EventRecord_
{

    public static volatile SingularAttribute<AlertRecord, Integer>                      severity;
    public static volatile SingularAttribute<AlertRecord, AlertDefinitionVersionRecord> alertDefinitionVersion;
    public static volatile ListAttribute<AlertRecord, AlertHistoryRecord>               history;
    public static volatile SingularAttribute<AlertRecord, AlertType>                    type;
    public static volatile SingularAttribute<AlertRecord, AlertStatus>                  status;

    public static final String SEVERITY                 = "severity";
    public static final String ALERT_DEFINITION_VERSION = "alertDefinitionVersion";
    public static final String HISTORY                  = "history";
    public static final String TYPE                     = "type";
    public static final String STATUS                   = "status";
}

