/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.persistence;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(HashedPassword.class)
public abstract class HashedPassword_
{

    public static volatile SingularAttribute<HashedPassword, byte[]> salt;
    public static volatile SingularAttribute<HashedPassword, byte[]> hash;

    public static final String SALT = "salt";
    public static final String HASH = "hash";
}

