/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.tags;

import java.util.BitSet;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.logic.tags.TagsQueryContext;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;

@JsonTypeName("TagsConditionMetrics")
public class TagsConditionMetrics extends TagsCondition
{
    public String metricsSysId;

    //--//

    public static TagsConditionMetrics build(String sysId)
    {
        TagsConditionMetrics query = new TagsConditionMetrics();
        query.metricsSysId = sysId;
        return query;
    }

    //--//

    @Override
    public void validate(StringBuilder path)
    {
        if (metricsSysId == null)
        {
            throw validationFailure(path, "metricsSysId");
        }
    }

    @Override
    public BitSet evaluate(TagsQueryContext context)
    {
        return context.lookupTag(AssetRecord.WellKnownTags.sysMetrics, metricsSysId);
    }
}
