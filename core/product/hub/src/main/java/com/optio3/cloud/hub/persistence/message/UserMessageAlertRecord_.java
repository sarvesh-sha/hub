/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.hub.persistence.message;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

import com.optio3.cloud.hub.persistence.alert.AlertRecord;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(UserMessageAlertRecord.class)
public abstract class UserMessageAlertRecord_ extends com.optio3.cloud.hub.persistence.message.UserMessageRecord_
{

    public static volatile SingularAttribute<UserMessageAlertRecord, AlertRecord> alert;

    public static final String ALERT = "alert";
}

