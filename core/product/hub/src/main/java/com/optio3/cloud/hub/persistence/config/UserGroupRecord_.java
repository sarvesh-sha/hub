/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.hub.persistence.config;

import javax.annotation.Generated;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(UserGroupRecord.class)
public abstract class UserGroupRecord_ extends com.optio3.cloud.persistence.RecordWithMetadata_
{

    public static volatile ListAttribute<UserGroupRecord, RoleRecord>      roles;
    public static volatile SetAttribute<UserGroupRecord, UserRecord>       members;
    public static volatile SingularAttribute<UserGroupRecord, String>      name;
    public static volatile ListAttribute<UserGroupRecord, UserGroupRecord> subGroups;
    public static volatile SingularAttribute<UserGroupRecord, String>      description;

    public static final String ROLES       = "roles";
    public static final String MEMBERS     = "members";
    public static final String NAME        = "name";
    public static final String SUB_GROUPS  = "subGroups";
    public static final String DESCRIPTION = "description";
}

