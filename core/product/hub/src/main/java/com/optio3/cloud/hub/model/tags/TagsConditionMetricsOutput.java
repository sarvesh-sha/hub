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

@JsonTypeName("TagsConditionMetricsOutput")
public class TagsConditionMetricsOutput extends TagsCondition
{
    public String metricsOutput;

    //--//

    public static TagsConditionMetricsOutput build(String name)
    {
        TagsConditionMetricsOutput query = new TagsConditionMetricsOutput();
        query.metricsOutput = name;
        return query;
    }

    //--//

    @Override
    public void validate(StringBuilder path)
    {
        if (metricsOutput == null)
        {
            throw validationFailure(path, "metricsOutput");
        }
    }

    @Override
    public BitSet evaluate(TagsQueryContext context)
    {
        return context.lookupTag(AssetRecord.WellKnownTags.sysMetricsOutput, metricsOutput);
    }
}
