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
@StaticMetamodel(DeviceElementRecord.class)
public abstract class DeviceElementRecord_ extends com.optio3.cloud.hub.persistence.asset.AssetRecord_
{

    public static volatile SingularAttribute<DeviceElementRecord, String> identifier;
    public static volatile SingularAttribute<DeviceElementRecord, String> contents;
    public static volatile SingularAttribute<DeviceElementRecord, String> samplingSettings;
    public static volatile SingularAttribute<DeviceElementRecord, byte[]> binaryRanges;

    public static final String IDENTIFIER        = "identifier";
    public static final String CONTENTS          = "contents";
    public static final String SAMPLING_SETTINGS = "samplingSettings";
    public static final String BINARY_RANGES     = "binaryRanges";
}

