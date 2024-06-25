/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.tags;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.cloud.exception.InvalidArgumentException;

public class TagsJoinQuery
{
    public final List<TagsJoinTerm> terms = Lists.newArrayList();
    public final List<TagsJoin>     joins = Lists.newArrayList();

    public int startOffset;
    public int maxResults;

    public static void validate(TagsJoinQuery query)
    {
        if (query == null)
        {
            throw new InvalidArgumentException("No query");
        }

        for (TagsJoinTerm term : query.terms)
        {
            if (term.conditions != null)
            {
                TagsCondition.validate(term.conditions);
            }
        }
    }
}
