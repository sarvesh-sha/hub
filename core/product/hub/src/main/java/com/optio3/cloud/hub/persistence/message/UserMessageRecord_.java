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

import com.optio3.cloud.hub.persistence.config.UserRecord;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(UserMessageRecord.class)
public abstract class UserMessageRecord_ extends com.optio3.cloud.persistence.RecordWithCommonFields_
{

    public static volatile SingularAttribute<UserMessageRecord, Boolean>    flagRead;
    public static volatile SingularAttribute<UserMessageRecord, String>     subject;
    public static volatile SingularAttribute<UserMessageRecord, Boolean>    flagActive;
    public static volatile SingularAttribute<UserMessageRecord, Boolean>    flagNew;
    public static volatile SingularAttribute<UserMessageRecord, String>     body;
    public static volatile SingularAttribute<UserMessageRecord, UserRecord> user;

    public static final String FLAG_READ   = "flagRead";
    public static final String SUBJECT     = "subject";
    public static final String FLAG_ACTIVE = "flagActive";
    public static final String FLAG_NEW    = "flagNew";
    public static final String BODY        = "body";
    public static final String USER        = "user";
}

