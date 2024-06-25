/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.builder.persistence.customer;

import javax.annotation.Generated;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(CustomerRecord.class)
public abstract class CustomerRecord_ extends com.optio3.cloud.persistence.RecordWithCommonFields_
{

    public static volatile ListAttribute<CustomerRecord, CustomerSharedUserRecord>   sharedUsers;
    public static volatile ListAttribute<CustomerRecord, CustomerSharedSecretRecord> sharedSecrets;
    public static volatile SingularAttribute<CustomerRecord, String>                 cloudId;
    public static volatile SingularAttribute<CustomerRecord, String>                 name;
    public static volatile ListAttribute<CustomerRecord, CustomerServiceRecord>      services;

    public static final String SHARED_USERS   = "sharedUsers";
    public static final String SHARED_SECRETS = "sharedSecrets";
    public static final String CLOUD_ID       = "cloudId";
    public static final String NAME           = "name";
    public static final String SERVICES       = "services";
}

