/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.asset;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.hub.model.metrics.MetricsBinding;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.MetricsDeviceElementRecord;

@JsonTypeName("MetricsDeviceElement")
public class MetricsDeviceElement extends DeviceElement
{
    @Optio3MapAsReadOnly
    public MetricsBinding bindings;

    //--//

    @Override
    public AssetRecord newRecord()
    {
        return new MetricsDeviceElementRecord();
    }
}
