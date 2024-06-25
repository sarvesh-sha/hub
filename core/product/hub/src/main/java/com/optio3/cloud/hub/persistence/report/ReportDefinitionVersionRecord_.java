/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.hub.persistence.report;

import javax.annotation.Generated;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(ReportDefinitionVersionRecord.class)
public abstract class ReportDefinitionVersionRecord_ extends com.optio3.cloud.persistence.RecordWithSequenceNumber_
{

    public static volatile ListAttribute<ReportDefinitionVersionRecord, ReportDefinitionVersionRecord>     successors;
    public static volatile SingularAttribute<ReportDefinitionVersionRecord, String>                        details;
    public static volatile SingularAttribute<ReportDefinitionVersionRecord, ReportDefinitionRecord>        definition;
    public static volatile SingularAttribute<ReportDefinitionVersionRecord, ReportDefinitionVersionRecord> predecessor;
    public static volatile SingularAttribute<ReportDefinitionVersionRecord, Integer>                       version;

    public static final String SUCCESSORS  = "successors";
    public static final String DETAILS     = "details";
    public static final String DEFINITION  = "definition";
    public static final String PREDECESSOR = "predecessor";
    public static final String VERSION     = "version";
}

