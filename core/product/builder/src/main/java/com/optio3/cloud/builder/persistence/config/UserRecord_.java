/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.builder.persistence.config;

import javax.annotation.Generated;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

import com.optio3.cloud.persistence.HashedPassword;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(UserRecord.class)
public abstract class UserRecord_ extends com.optio3.cloud.persistence.RecordWithCommonFields_
{

    public static volatile SingularAttribute<UserRecord, String>           firstName;
    public static volatile SingularAttribute<UserRecord, String>           lastName;
    public static volatile SingularAttribute<UserRecord, String>           emailAddress;
    public static volatile SingularAttribute<UserRecord, HashedPassword>   password;
    public static volatile ListAttribute<UserRecord, UserPreferenceRecord> preferences;
    public static volatile SingularAttribute<UserRecord, String>           phoneNumber;
    public static volatile ListAttribute<UserRecord, RoleRecord>           roles;
    public static volatile SingularAttribute<UserRecord, Integer>          identityVersion;
    public static volatile SingularAttribute<UserRecord, String>           resetToken;
    public static volatile SingularAttribute<UserRecord, Boolean>          fromLdap;

    public static final String FIRST_NAME       = "firstName";
    public static final String LAST_NAME        = "lastName";
    public static final String EMAIL_ADDRESS    = "emailAddress";
    public static final String PASSWORD         = "password";
    public static final String PREFERENCES      = "preferences";
    public static final String PHONE_NUMBER     = "phoneNumber";
    public static final String ROLES            = "roles";
    public static final String IDENTITY_VERSION = "identityVersion";
    public static final String RESET_TOKEN      = "resetToken";
    public static final String FROM_LDAP        = "fromLdap";
}

