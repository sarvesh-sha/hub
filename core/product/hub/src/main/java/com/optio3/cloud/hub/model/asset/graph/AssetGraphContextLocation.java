/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.asset.graph;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.model.tags.TagsCondition;
import com.optio3.cloud.hub.model.tags.TagsConditionLocation;

@JsonTypeName("AssetGraphContextLocation")
public class AssetGraphContextLocation extends AssetGraphContext
{
    public String locationSysId;

    @Override
    public TagsCondition getRootCondition()
    {
        return TagsConditionLocation.build(locationSysId);
    }
}
