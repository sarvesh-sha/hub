/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.asset;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.LogicalAssetRecord;

@JsonTypeName("LogicalAsset")
public class LogicalAsset extends Asset
{
    @Override
    public AssetRecord newRecord()
    {
        return new LogicalAssetRecord();
    }
}
