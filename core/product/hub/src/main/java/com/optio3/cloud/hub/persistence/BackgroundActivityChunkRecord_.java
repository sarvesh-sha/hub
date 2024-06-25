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
@StaticMetamodel(BackgroundActivityChunkRecord.class)
public abstract class BackgroundActivityChunkRecord_ extends com.optio3.cloud.persistence.RecordForBackgroundActivityChunk_
{

    public static volatile SingularAttribute<BackgroundActivityChunkRecord, BackgroundActivityRecord> owningActivity;

    public static final String OWNING_ACTIVITY = "owningActivity";
}

