/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.hub.persistence.report;

import java.time.ZonedDateTime;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

import com.optio3.cloud.hub.model.report.ReportReason;
import com.optio3.cloud.hub.model.report.ReportStatus;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(ReportRecord.class)
public abstract class ReportRecord_ extends com.optio3.cloud.persistence.RecordWithCommonFields_
{

    public static volatile SingularAttribute<ReportRecord, ReportReason>                  reason;
    public static volatile SingularAttribute<ReportRecord, ReportDefinitionRecord>        reportDefinition;
    public static volatile SingularAttribute<ReportRecord, ZonedDateTime>                 rangeStart;
    public static volatile SingularAttribute<ReportRecord, Integer>                       size;
    public static volatile SingularAttribute<ReportRecord, byte[]>                        bytes;
    public static volatile SingularAttribute<ReportRecord, ReportDefinitionVersionRecord> reportDefinitionVersion;
    public static volatile SingularAttribute<ReportRecord, ZonedDateTime>                 rangeEnd;
    public static volatile SingularAttribute<ReportRecord, ReportStatus>                  status;

    public static final String REASON                    = "reason";
    public static final String REPORT_DEFINITION         = "reportDefinition";
    public static final String RANGE_START               = "rangeStart";
    public static final String SIZE                      = "size";
    public static final String BYTES                     = "bytes";
    public static final String REPORT_DEFINITION_VERSION = "reportDefinitionVersion";
    public static final String RANGE_END                 = "rangeEnd";
    public static final String STATUS                    = "status";
}

