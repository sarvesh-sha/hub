/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.builder.persistence.customer;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

import com.optio3.cloud.persistence.EncryptedPayload;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(CustomerSharedUserRecord.class)
public abstract class CustomerSharedUserRecord_ extends com.optio3.cloud.persistence.RecordWithCommonFields_
{

    public static volatile SingularAttribute<CustomerSharedUserRecord, String>           firstName;
    public static volatile SingularAttribute<CustomerSharedUserRecord, String>           lastName;
    public static volatile SingularAttribute<CustomerSharedUserRecord, String>           emailAddress;
    public static volatile SingularAttribute<CustomerSharedUserRecord, EncryptedPayload> password;
    public static volatile SingularAttribute<CustomerSharedUserRecord, String>           phoneNumber;
    public static volatile SingularAttribute<CustomerSharedUserRecord, String>           roles;
    public static volatile SingularAttribute<CustomerSharedUserRecord, CustomerRecord>   customer;

    public static final String FIRST_NAME    = "firstName";
    public static final String LAST_NAME     = "lastName";
    public static final String EMAIL_ADDRESS = "emailAddress";
    public static final String PASSWORD      = "password";
    public static final String PHONE_NUMBER  = "phoneNumber";
    public static final String ROLES         = "roles";
    public static final String CUSTOMER      = "customer";
}

