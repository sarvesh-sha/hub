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

@JsonTypeName("TagsConditionPointClass")
public class TagsConditionPointClass extends TagsCondition
{
    public String pointClass;

    //--//

    public static TagsConditionPointClass build(String pointClass)
    {
        TagsConditionPointClass query = new TagsConditionPointClass();
        query.pointClass = pointClass;
        return query;
    }

    //--//

    @Override
    public void validate(StringBuilder path)
    {
        if (pointClass == null)
        {
            throw validationFailure(path, "pointClass");
        }
    }

    @Override
    public BitSet evaluate(TagsQueryContext context)
    {
        return context.lookupTag(AssetRecord.WellKnownTags.pointClassId, pointClass);
    }
}
