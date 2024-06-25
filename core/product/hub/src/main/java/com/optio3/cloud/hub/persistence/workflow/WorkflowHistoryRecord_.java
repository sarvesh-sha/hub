/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.hub.persistence.workflow;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

import com.optio3.cloud.hub.model.workflow.WorkflowEventType;
import com.optio3.cloud.hub.persistence.config.UserRecord;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(WorkflowHistoryRecord.class)
public abstract class WorkflowHistoryRecord_ extends com.optio3.cloud.persistence.RecordWithCommonFields_
{

    public static volatile SingularAttribute<WorkflowHistoryRecord, WorkflowRecord>    workflow;
    public static volatile SingularAttribute<WorkflowHistoryRecord, String>            text;
    public static volatile SingularAttribute<WorkflowHistoryRecord, WorkflowEventType> type;
    public static volatile SingularAttribute<WorkflowHistoryRecord, UserRecord>        user;

    public static final String WORKFLOW = "workflow";
    public static final String TEXT     = "text";
    public static final String TYPE     = "type";
    public static final String USER     = "user";
}

