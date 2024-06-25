/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.hub.persistence.acl;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(RecordWithAccessControlList.class)
public abstract class RecordWithAccessControlList_ extends com.optio3.cloud.persistence.RecordWithCommonFields_
{

    public static volatile SingularAttribute<RecordWithAccessControlList, Integer> sysAcl;
    public static volatile SingularAttribute<RecordWithAccessControlList, Integer> sysAclEffective;

    public static final String SYS_ACL           = "sysAcl";
    public static final String SYS_ACL_EFFECTIVE = "sysAclEffective";
}

