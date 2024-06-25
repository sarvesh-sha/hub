/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.asset.graph;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.model.tags.TagsCondition;
import com.optio3.cloud.hub.model.tags.TagsConditionIsAsset;

@JsonTypeName("AssetGraphContextAsset")
public class AssetGraphContextAsset extends AssetGraphContext
{
    public String sysId;

    @Override
    public TagsCondition getRootCondition()
    {
        return TagsConditionIsAsset.build(sysId);
    }
}
