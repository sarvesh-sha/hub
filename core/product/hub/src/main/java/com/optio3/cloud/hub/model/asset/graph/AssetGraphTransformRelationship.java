/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.asset.graph;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.model.asset.AssetRelationship;

@JsonTypeName("AssetGraphTransformRelationship")
public class AssetGraphTransformRelationship extends AssetGraphTransform
{
    public AssetRelationship relationship;
}
