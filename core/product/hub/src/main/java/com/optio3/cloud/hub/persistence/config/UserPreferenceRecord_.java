/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.hub.persistence.config;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(UserPreferenceRecord.class)
public abstract class UserPreferenceRecord_ extends com.optio3.cloud.persistence.RecordWithCommonFields_
{

    public static volatile SingularAttribute<UserPreferenceRecord, UserRecord> user;
    public static volatile SingularAttribute<UserPreferenceRecord, String>     value;
    public static volatile SingularAttribute<UserPreferenceRecord, String>     key;

    public static final String USER  = "user";
    public static final String VALUE = "value";
    public static final String KEY   = "key";
}

