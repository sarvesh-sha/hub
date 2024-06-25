/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.hub.persistence.alert;

import java.time.ZonedDateTime;

import javax.annotation.Generated;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

import com.optio3.cloud.hub.model.alert.AlertDefinitionPurpose;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(AlertDefinitionRecord.class)
public abstract class AlertDefinitionRecord_ extends com.optio3.cloud.persistence.RecordWithCommonFields_
{

    public static volatile SingularAttribute<AlertDefinitionRecord, byte[]>                       logRanges;
    public static volatile SingularAttribute<AlertDefinitionRecord, AlertDefinitionPurpose>       purpose;
    public static volatile ListAttribute<AlertDefinitionRecord, AlertDefinitionVersionRecord>     versions;
    public static volatile SingularAttribute<AlertDefinitionRecord, AlertDefinitionVersionRecord> releaseVersion;
    public static volatile SingularAttribute<AlertDefinitionRecord, byte[]>                       executionState;
    public static volatile SingularAttribute<AlertDefinitionRecord, String>                       description;
    public static volatile SingularAttribute<AlertDefinitionRecord, Boolean>                      active;
    public static volatile SingularAttribute<AlertDefinitionRecord, Integer>                      lastOffset;
    public static volatile SingularAttribute<AlertDefinitionRecord, AlertDefinitionVersionRecord> headVersion;
    public static volatile SingularAttribute<AlertDefinitionRecord, String>                       title;
    public static volatile SingularAttribute<AlertDefinitionRecord, ZonedDateTime>                lastOutput;

    public static final String LOG_RANGES      = "logRanges";
    public static final String PURPOSE         = "purpose";
    public static final String VERSIONS        = "versions";
    public static final String RELEASE_VERSION = "releaseVersion";
    public static final String EXECUTION_STATE = "executionState";
    public static final String DESCRIPTION     = "description";
    public static final String ACTIVE          = "active";
    public static final String LAST_OFFSET     = "lastOffset";
    public static final String HEAD_VERSION    = "headVersion";
    public static final String TITLE           = "title";
    public static final String LAST_OUTPUT     = "lastOutput";
}

