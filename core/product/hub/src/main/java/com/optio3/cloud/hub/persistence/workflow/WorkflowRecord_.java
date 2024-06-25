/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.hub.persistence.workflow;

import javax.annotation.Generated;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

import com.optio3.cloud.hub.model.workflow.WorkflowStatus;
import com.optio3.cloud.hub.model.workflow.WorkflowType;
import com.optio3.cloud.hub.persistence.config.UserRecord;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(WorkflowRecord.class)
public abstract class WorkflowRecord_ extends com.optio3.cloud.hub.persistence.event.EventRecord_
{

    public static volatile SingularAttribute<WorkflowRecord, UserRecord>        createdBy;
    public static volatile SingularAttribute<WorkflowRecord, byte[]>            details;
    public static volatile ListAttribute<WorkflowRecord, WorkflowHistoryRecord> history;
    public static volatile SingularAttribute<WorkflowRecord, WorkflowType>      type;
    public static volatile SingularAttribute<WorkflowRecord, Integer>           priority;
    public static volatile SingularAttribute<WorkflowRecord, UserRecord>        assignedTo;
    public static volatile SingularAttribute<WorkflowRecord, WorkflowStatus>    status;

    public static final String CREATED_BY  = "createdBy";
    public static final String DETAILS     = "details";
    public static final String HISTORY     = "history";
    public static final String TYPE        = "type";
    public static final String PRIORITY    = "priority";
    public static final String ASSIGNED_TO = "assignedTo";
    public static final String STATUS      = "status";
}

