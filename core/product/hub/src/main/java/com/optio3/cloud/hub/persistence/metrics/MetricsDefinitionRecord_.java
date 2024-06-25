/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.hub.persistence.metrics;

import javax.annotation.Generated;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

import com.optio3.cloud.hub.persistence.asset.MetricsDeviceElementRecord;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(MetricsDefinitionRecord.class)
public abstract class MetricsDefinitionRecord_ extends com.optio3.cloud.persistence.RecordWithCommonFields_
{

    public static volatile ListAttribute<MetricsDefinitionRecord, MetricsDefinitionVersionRecord>     versions;
    public static volatile SingularAttribute<MetricsDefinitionRecord, MetricsDefinitionVersionRecord> releaseVersion;
    public static volatile SetAttribute<MetricsDefinitionRecord, MetricsDeviceElementRecord>          syntheticAssets;
    public static volatile SingularAttribute<MetricsDefinitionRecord, String>                         description;
    public static volatile SingularAttribute<MetricsDefinitionRecord, MetricsDefinitionVersionRecord> headVersion;
    public static volatile SingularAttribute<MetricsDefinitionRecord, String>                         title;

    public static final String VERSIONS         = "versions";
    public static final String RELEASE_VERSION  = "releaseVersion";
    public static final String SYNTHETIC_ASSETS = "syntheticAssets";
    public static final String DESCRIPTION      = "description";
    public static final String HEAD_VERSION     = "headVersion";
    public static final String TITLE            = "title";
}

