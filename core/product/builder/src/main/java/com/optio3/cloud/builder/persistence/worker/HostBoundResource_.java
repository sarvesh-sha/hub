/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.builder.persistence.worker;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(HostBoundResource.class)
public abstract class HostBoundResource_ extends com.optio3.cloud.builder.persistence.worker.TrackedRecordWithResources_
{

    public static volatile SingularAttribute<HostBoundResource, HostRecord> owningHost;

    public static final String OWNING_HOST = "owningHost";
}

