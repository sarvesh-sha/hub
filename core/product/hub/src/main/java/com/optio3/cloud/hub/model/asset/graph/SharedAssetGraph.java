/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.asset.graph;

import com.optio3.cloud.annotation.Optio3IncludeInApiDefinitions;

@Optio3IncludeInApiDefinitions
public class SharedAssetGraph
{
    public String     id;
    public String     name;
    public AssetGraph graph;
}
