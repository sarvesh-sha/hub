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

import com.optio3.cloud.hub.model.audit.AuditType;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(AuditRecord.class)
public abstract class AuditRecord_ extends com.optio3.cloud.hub.persistence.event.EventRecord_
{

    public static volatile SingularAttribute<AuditRecord, AuditType> type;

    public static final String TYPE = "type";
}

