/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.asset;

public class AssetRelationshipRequest
{
    public String            assetId;
    public AssetRelationship relationship;
    public boolean           fromParentToChildren;
}
