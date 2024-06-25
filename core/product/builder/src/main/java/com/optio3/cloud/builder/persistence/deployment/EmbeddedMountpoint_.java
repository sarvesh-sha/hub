/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.builder.persistence.deployment;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(EmbeddedMountpoint.class)
public abstract class EmbeddedMountpoint_
{

    public static volatile SingularAttribute<EmbeddedMountpoint, String>  mode;
    public static volatile SingularAttribute<EmbeddedMountpoint, String>  propagation;
    public static volatile SingularAttribute<EmbeddedMountpoint, String>  driver;
    public static volatile SingularAttribute<EmbeddedMountpoint, String>  name;
    public static volatile SingularAttribute<EmbeddedMountpoint, String>  destination;
    public static volatile SingularAttribute<EmbeddedMountpoint, Boolean> readWrite;
    public static volatile SingularAttribute<EmbeddedMountpoint, String>  source;
    public static volatile SingularAttribute<EmbeddedMountpoint, String>  type;

    public static final String MODE        = "mode";
    public static final String PROPAGATION = "propagation";
    public static final String DRIVER      = "driver";
    public static final String NAME        = "name";
    public static final String DESTINATION = "destination";
    public static final String READ_WRITE  = "readWrite";
    public static final String SOURCE      = "source";
    public static final String TYPE        = "type";
}

