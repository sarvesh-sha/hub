/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.hub.model.asset.Asset;
import com.optio3.cloud.hub.persistence.HostAssetRecord;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;

@JsonTypeName("HostAsset")
public class HostAsset extends Asset
{
    @Optio3MapAsReadOnly
    public ZonedDateTime lastOutput;

    @Optio3MapAsReadOnly
    public int lastOffset;

    //--//

    @Override
    public AssetRecord newRecord()
    {
        return new HostAssetRecord();
    }
}
