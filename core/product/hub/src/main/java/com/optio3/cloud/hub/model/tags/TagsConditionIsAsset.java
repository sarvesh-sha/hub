/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.tags;

import java.util.BitSet;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.logic.tags.TagsQueryContext;

@JsonTypeName("TagsConditionIsAsset")
public class TagsConditionIsAsset extends TagsCondition
{
    public String sysId;

    //--//

    public static TagsConditionIsAsset build(String sysId)
    {
        TagsConditionIsAsset query = new TagsConditionIsAsset();
        query.sysId = sysId;
        return query;
    }

    //--//

    @Override
    public void validate(StringBuilder path)
    {
        if (sysId == null)
        {
            throw validationFailure(path, "sysId");
        }
    }

    @Override
    public BitSet evaluate(TagsQueryContext context)
    {
        return context.lookupAsset(sysId);
    }
}
