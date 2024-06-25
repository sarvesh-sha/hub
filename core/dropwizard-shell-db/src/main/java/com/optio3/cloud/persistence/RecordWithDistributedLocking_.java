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
@StaticMetamodel(RecordWithDistributedLocking.class)
public abstract class RecordWithDistributedLocking_
{

    public static volatile SingularAttribute<RecordWithDistributedLocking, String>        lockId;
    public static volatile SingularAttribute<RecordWithDistributedLocking, String>        heldBy;
    public static volatile SingularAttribute<RecordWithDistributedLocking, ZonedDateTime> heldOn;

    public static final String LOCK_ID = "lockId";
    public static final String HELD_BY = "heldBy";
    public static final String HELD_ON = "heldOn";
}

