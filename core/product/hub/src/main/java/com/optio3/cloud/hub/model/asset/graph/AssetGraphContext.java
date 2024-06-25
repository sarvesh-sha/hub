/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.asset.graph;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.optio3.cloud.hub.model.tags.TagsCondition;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes({ @JsonSubTypes.Type(value = AssetGraphContextAsset.class),
                @JsonSubTypes.Type(value = AssetGraphContextAssets.class),
                @JsonSubTypes.Type(value = AssetGraphContextLocation.class),
                @JsonSubTypes.Type(value = AssetGraphContextLocations.class) })
public abstract class AssetGraphContext
{
    public String graphId;

    public String nodeId;

    @JsonIgnore
    public abstract TagsCondition getRootCondition();
}
