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
@StaticMetamodel(CustomerSharedSecretRecord.class)
public abstract class CustomerSharedSecretRecord_ extends com.optio3.cloud.persistence.RecordWithCommonFields_
{

    public static volatile SingularAttribute<CustomerSharedSecretRecord, String>           context;
    public static volatile SingularAttribute<CustomerSharedSecretRecord, EncryptedPayload> value;
    public static volatile SingularAttribute<CustomerSharedSecretRecord, String>           key;
    public static volatile SingularAttribute<CustomerSharedSecretRecord, CustomerRecord>   customer;

    public static final String CONTEXT  = "context";
    public static final String VALUE    = "value";
    public static final String KEY      = "key";
    public static final String CUSTOMER = "customer";
}

