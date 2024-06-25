/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.asset;

import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.model.TypedRecordIdentityList;

public class AssetRelationshipResponse
{
    public final TypedRecordIdentityList<AssetRecord> assets = new TypedRecordIdentityList<>();
}
