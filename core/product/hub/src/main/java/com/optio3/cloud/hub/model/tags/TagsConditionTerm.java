/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.tags;

import java.util.BitSet;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.logic.tags.TagsQueryContext;

@JsonTypeName("TagsConditionTerm")
public class TagsConditionTerm extends TagsCondition
{
    public String tag;

    //--//

    public static TagsConditionTerm build(String tag)
    {
        TagsConditionTerm query = new TagsConditionTerm();
        query.tag = tag;
        return query;
    }

    //--//

    @Override
    public void validate(StringBuilder path)
    {
        if (tag == null)
        {
            throw validationFailure(path, "tag");
        }
    }

    @Override
    public BitSet evaluate(TagsQueryContext context)
    {
        return context.lookupTag(tag);
    }
}
