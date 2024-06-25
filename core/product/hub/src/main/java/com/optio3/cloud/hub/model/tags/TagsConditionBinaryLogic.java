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
import com.optio3.util.Exceptions;

@JsonTypeName("TagsConditionBinaryLogic")
public class TagsConditionBinaryLogic extends TagsConditionBinary
{
    public TagsConditionOperator op;

    //--//

    public static TagsConditionBinaryLogic build(TagsCondition a,
                                                 TagsCondition b,
                                                 TagsConditionOperator op)
    {
        TagsConditionBinaryLogic query = new TagsConditionBinaryLogic();
        query.a  = a;
        query.b  = b;
        query.op = op;
        return query;
    }

    //--//

    @Override
    public void validate(StringBuilder path)
    {
        validateChild(path, "a", a);
        validateChild(path, "b", b);
    }

    @Override
    public BitSet evaluate(TagsQueryContext context)
    {
        BitSet bsA = a.evaluate(context);
        BitSet bsB = b.evaluate(context);

        switch (op)
        {
            case Or:
                return BitSets.or(bsA, bsB);

            case And:
                return BitSets.and(bsA, bsB);

            case Xor:
                return BitSets.xor(bsA, bsB);
        }

        throw Exceptions.newIllegalArgumentException("Unknown operator: %s", op);
    }
}
