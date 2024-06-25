/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.hub.persistence.acl;

import java.time.ZonedDateTime;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(AccessControlListRecord.class)
public abstract class AccessControlListRecord_
{

    public static volatile SingularAttribute<AccessControlListRecord, byte[]>        policyHash;
    public static volatile SingularAttribute<AccessControlListRecord, Integer>       sysSeq;
    public static volatile SingularAttribute<AccessControlListRecord, ZonedDateTime> updatedOn;
    public static volatile SingularAttribute<AccessControlListRecord, ZonedDateTime> createdOn;
    public static volatile SingularAttribute<AccessControlListRecord, byte[]>        policy;

    public static final String POLICY_HASH = "policyHash";
    public static final String SYS_SEQ     = "sysSeq";
    public static final String UPDATED_ON  = "updatedOn";
    public static final String CREATED_ON  = "createdOn";
    public static final String POLICY      = "policy";
}

