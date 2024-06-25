/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.asset.graph;

import org.apache.commons.lang3.StringUtils;

public class AssetGraphBinding
{
    public String graphId;

    public String nodeId;

    // TODO: UPGRADE PATCH: Legacy fixup for renamed field
    public void setId(String id)
    {
        if (!StringUtils.isEmpty(id))
        {
            nodeId = id;
        }
    }

    public String selectorId;
}
