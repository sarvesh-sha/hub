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
@StaticMetamodel(DeviceRecord.class)
public abstract class DeviceRecord_ extends com.optio3.cloud.hub.persistence.asset.AssetRecord_
{

    public static volatile SingularAttribute<DeviceRecord, String> modelName;
    public static volatile SingularAttribute<DeviceRecord, String> identityDescriptor;
    public static volatile SingularAttribute<DeviceRecord, String> manufacturerName;
    public static volatile SingularAttribute<DeviceRecord, String> firmwareVersion;
    public static volatile SingularAttribute<DeviceRecord, String> productName;

    public static final String MODEL_NAME          = "modelName";
    public static final String IDENTITY_DESCRIPTOR = "identityDescriptor";
    public static final String MANUFACTURER_NAME   = "manufacturerName";
    public static final String FIRMWARE_VERSION    = "firmwareVersion";
    public static final String PRODUCT_NAME        = "productName";
}

