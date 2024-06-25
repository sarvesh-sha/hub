/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.hub.persistence.dashboard;

import javax.annotation.Generated;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

import com.optio3.cloud.hub.persistence.config.UserRecord;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(DashboardDefinitionRecord.class)
public abstract class DashboardDefinitionRecord_ extends com.optio3.cloud.hub.persistence.acl.RecordWithAccessControlList_
{

    public static volatile ListAttribute<DashboardDefinitionRecord, DashboardDefinitionVersionRecord>     versions;
    public static volatile SingularAttribute<DashboardDefinitionRecord, DashboardDefinitionVersionRecord> releaseVersion;
    public static volatile SingularAttribute<DashboardDefinitionRecord, DashboardDefinitionVersionRecord> headVersion;
    public static volatile SingularAttribute<DashboardDefinitionRecord, UserRecord>                       user;

    public static final String VERSIONS        = "versions";
    public static final String RELEASE_VERSION = "releaseVersion";
    public static final String HEAD_VERSION    = "headVersion";
    public static final String USER            = "user";
}

