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

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(DashboardDefinitionVersionRecord.class)
public abstract class DashboardDefinitionVersionRecord_ extends com.optio3.cloud.persistence.RecordWithSequenceNumber_
{

    public static volatile ListAttribute<DashboardDefinitionVersionRecord, DashboardDefinitionVersionRecord>     successors;
    public static volatile SingularAttribute<DashboardDefinitionVersionRecord, String>                           details;
    public static volatile SingularAttribute<DashboardDefinitionVersionRecord, DashboardDefinitionRecord>        definition;
    public static volatile SingularAttribute<DashboardDefinitionVersionRecord, DashboardDefinitionVersionRecord> predecessor;
    public static volatile SingularAttribute<DashboardDefinitionVersionRecord, Integer>                          version;

    public static final String SUCCESSORS  = "successors";
    public static final String DETAILS     = "details";
    public static final String DEFINITION  = "definition";
    public static final String PREDECESSOR = "predecessor";
    public static final String VERSION     = "version";
}

