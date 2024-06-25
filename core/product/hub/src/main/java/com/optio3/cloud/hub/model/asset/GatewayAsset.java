/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.asset;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.GatewayAssetRecord;

@JsonTypeName("GatewayAsset")
public class GatewayAsset extends Asset
{
    @Optio3MapAsReadOnly
    public String instanceId;

    //--//

    public int warningThreshold;

    public int alertThreshold;

    @Optio3MapAsReadOnly
    public int cpuLoadLast4Hours;

    @Optio3MapAsReadOnly
    public int cpuLoadPrevious4Hours;

    //--//

    @Optio3MapAsReadOnly
    public GatewayDetails details;

    @Optio3MapAsReadOnly
    public ZonedDateTime lastOutput;

    @Optio3MapAsReadOnly
    public int lastOffset;

    //--//

    @Override
    public AssetRecord newRecord()
    {
        return new GatewayAssetRecord();
    }
}
