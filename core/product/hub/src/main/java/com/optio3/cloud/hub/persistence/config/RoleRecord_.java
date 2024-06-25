/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.hub.persistence.config;

import javax.annotation.Generated;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(RoleRecord.class)
public abstract class RoleRecord_ extends com.optio3.cloud.persistence.RecordWithCommonFields_
{

    public static volatile SingularAttribute<RoleRecord, String>  displayName;
    public static volatile SetAttribute<RoleRecord, UserRecord>   members;
    public static volatile SingularAttribute<RoleRecord, String>  name;
    public static volatile SingularAttribute<RoleRecord, Boolean> removeAllowed;
    public static volatile SingularAttribute<RoleRecord, Boolean> addAllowed;

    public static final String DISPLAY_NAME   = "displayName";
    public static final String MEMBERS        = "members";
    public static final String NAME           = "name";
    public static final String REMOVE_ALLOWED = "removeAllowed";
    public static final String ADD_ALLOWED    = "addAllowed";
}

