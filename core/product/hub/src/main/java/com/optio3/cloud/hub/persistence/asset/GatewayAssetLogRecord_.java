/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.hub.persistence.asset;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(GatewayAssetLogRecord.class)
public abstract class GatewayAssetLogRecord_ extends com.optio3.cloud.persistence.CommonLogRecord_
{

    public static volatile SingularAttribute<GatewayAssetLogRecord, GatewayAssetRecord> owningGateway;

    public static final String OWNING_GATEWAY = "owningGateway";
}
