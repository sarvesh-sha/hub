/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.hub.persistence.report;

import java.time.ZonedDateTime;

import javax.annotation.Generated;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

import com.optio3.cloud.hub.persistence.config.UserRecord;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(ReportDefinitionRecord.class)
public abstract class ReportDefinitionRecord_ extends com.optio3.cloud.persistence.RecordForRecurringActivity_
{

    public static volatile ListAttribute<ReportDefinitionRecord, ReportRecord>                      reports;
    public static volatile ListAttribute<ReportDefinitionRecord, ReportDefinitionVersionRecord>     versions;
    public static volatile SingularAttribute<ReportDefinitionRecord, ReportDefinitionVersionRecord> releaseVersion;
    public static volatile SingularAttribute<ReportDefinitionRecord, ZonedDateTime>                 autoDelete;
    public static volatile SingularAttribute<ReportDefinitionRecord, String>                        description;
    public static volatile SingularAttribute<ReportDefinitionRecord, Boolean>                       active;
    public static volatile SingularAttribute<ReportDefinitionRecord, ReportDefinitionVersionRecord> headVersion;
    public static volatile SingularAttribute<ReportDefinitionRecord, UserRecord>                    user;

    public static final String REPORTS         = "reports";
    public static final String VERSIONS        = "versions";
    public static final String RELEASE_VERSION = "releaseVersion";
    public static final String AUTO_DELETE     = "autoDelete";
    public static final String DESCRIPTION     = "description";
    public static final String ACTIVE          = "active";
    public static final String HEAD_VERSION    = "headVersion";
    public static final String USER            = "user";
}

