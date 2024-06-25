/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.tags;

import java.util.BitSet;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.logic.tags.TagsQueryContext;

@JsonTypeName("TagsConditionTermWithValue")
public class TagsConditionTermWithValue extends TagsCondition
{
    public String tag;
    public String valueToMatch;

    //--//

    public static TagsConditionTermWithValue build(String tag,
                                                   String value)
    {
        TagsConditionTermWithValue query = new TagsConditionTermWithValue();
        query.tag          = tag;
        query.valueToMatch = value;
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
        return context.lookupTag(tag, valueToMatch);
    }
}
