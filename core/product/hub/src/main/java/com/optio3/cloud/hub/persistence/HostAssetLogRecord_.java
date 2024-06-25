/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.hub.persistence;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(HostAssetLogRecord.class)
public abstract class HostAssetLogRecord_ extends com.optio3.cloud.persistence.CommonLogRecord_
{

    public static volatile SingularAttribute<HostAssetLogRecord, HostAssetRecord> owningHost;

    public static final String OWNING_HOST = "owningHost";
}

