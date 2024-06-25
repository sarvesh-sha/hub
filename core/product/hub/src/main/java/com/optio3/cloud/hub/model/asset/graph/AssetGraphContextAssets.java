/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.asset.graph;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;
import com.optio3.cloud.hub.model.tags.TagsCondition;
import com.optio3.cloud.hub.model.tags.TagsConditionBinaryLogic;
import com.optio3.cloud.hub.model.tags.TagsConditionIsAsset;
import com.optio3.cloud.hub.model.tags.TagsConditionOperator;

@JsonTypeName("AssetGraphContextAssets")
public class AssetGraphContextAssets extends AssetGraphContext
{
    public List<String> sysIds = Lists.newArrayList();

    public boolean selectAll;

    @Override
    public TagsCondition getRootCondition()
    {
        TagsCondition logic = null;

        for (String sysId : sysIds)
        {
            TagsCondition logicNext = TagsConditionIsAsset.build(sysId);
            if (logic != null)
            {
                TagsConditionBinaryLogic logicMerge = new TagsConditionBinaryLogic();
                logicMerge.op = TagsConditionOperator.Or;
                logicMerge.a  = logic;
                logicMerge.b  = logicNext;

                logic = logicMerge;
            }
            else
            {
                logic = logicNext;
            }
        }

        return logic;
    }
}
