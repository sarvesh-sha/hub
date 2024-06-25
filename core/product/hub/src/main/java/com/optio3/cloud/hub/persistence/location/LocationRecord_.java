/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.hub.persistence.location;

import javax.annotation.Generated;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

import com.optio3.cloud.hub.model.location.LocationType;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(LocationRecord.class)
public abstract class LocationRecord_ extends com.optio3.cloud.hub.persistence.asset.AssetRecord_
{

    public static volatile SetAttribute<LocationRecord, AssetRecord>       assets;
    public static volatile SingularAttribute<LocationRecord, LocationType> type;

    public static final String ASSETS = "assets";
    public static final String TYPE   = "type";
}

