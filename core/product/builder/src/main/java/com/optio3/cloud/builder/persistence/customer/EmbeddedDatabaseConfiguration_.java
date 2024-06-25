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
@StaticMetamodel(EmbeddedDatabaseConfiguration.class)
public abstract class EmbeddedDatabaseConfiguration_
{

    public static volatile SingularAttribute<EmbeddedDatabaseConfiguration, DatabaseMode>     mode;
    public static volatile SingularAttribute<EmbeddedDatabaseConfiguration, String>           databaseUser;
    public static volatile SingularAttribute<EmbeddedDatabaseConfiguration, String>           server;
    public static volatile SingularAttribute<EmbeddedDatabaseConfiguration, String>           databaseName;
    public static volatile SingularAttribute<EmbeddedDatabaseConfiguration, EncryptedPayload> databasePassword;

    public static final String MODE              = "mode";
    public static final String DATABASE_USER     = "databaseUser";
    public static final String SERVER            = "server";
    public static final String DATABASE_NAME     = "databaseName";
    public static final String DATABASE_PASSWORD = "databasePassword";
}

