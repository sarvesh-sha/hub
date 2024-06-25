/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.persistence;

import java.time.ZonedDateTime;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(RecordWithCommonFields.class)
public abstract class RecordWithCommonFields_
{

    public static volatile SingularAttribute<RecordWithCommonFields, String>        sysId;
    public static volatile SingularAttribute<RecordWithCommonFields, ZonedDateTime> updatedOn;
    public static volatile SingularAttribute<RecordWithCommonFields, ZonedDateTime> createdOn;

    public static final String SYS_ID     = "sysId";
    public static final String UPDATED_ON = "updatedOn";
    public static final String CREATED_ON = "createdOn";
}

