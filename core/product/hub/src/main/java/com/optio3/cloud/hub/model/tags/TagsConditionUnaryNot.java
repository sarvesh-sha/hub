/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.tags;

import java.util.BitSet;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.logic.tags.TagsQueryContext;
import com.optio3.util.BitSets;

@JsonTypeName("TagsConditionUnaryNot")
public class TagsConditionUnaryNot extends TagsConditionUnary
{
    public static TagsConditionUnaryNot build(TagsCondition a)
    {
        TagsConditionUnaryNot query = new TagsConditionUnaryNot();
        query.a = a;
        return query;
    }

    //--//

    @Override
    public void validate(StringBuilder path)
    {
        validateChild(path, "a", a);
    }

    @Override
    public BitSet evaluate(TagsQueryContext context)
    {
        BitSet bsA = a.evaluate(context);

        return BitSets.not(bsA, context.getNumberOrRecords());
    }
}