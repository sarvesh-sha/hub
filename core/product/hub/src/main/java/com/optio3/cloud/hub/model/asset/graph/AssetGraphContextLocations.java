/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.asset.graph;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.model.tags.TagsCondition;
import com.optio3.cloud.hub.model.tags.TagsConditionBinaryLogic;
import com.optio3.cloud.hub.model.tags.TagsConditionLocation;
import com.optio3.cloud.hub.model.tags.TagsConditionOperator;

@JsonTypeName("AssetGraphContextLocations")
public class AssetGraphContextLocations extends AssetGraphContext
{
    public List<String> locationSysIds;

    @Override
    public TagsCondition getRootCondition()
    {
        TagsCondition logic = null;

        for (String locationId : locationSysIds)
        {
            TagsCondition logicNext = TagsConditionLocation.build(locationId);
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