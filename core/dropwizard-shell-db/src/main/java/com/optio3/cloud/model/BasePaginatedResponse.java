/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.model;

import java.util.List;

import com.google.common.collect.Lists;

public abstract class BasePaginatedResponse<T>
{
    public int version;
    public int offset;
    public int nextOffset;

    public final List<T> results = Lists.newArrayList();

    //--//

    public void handlePagination(BasePaginatedRequest request,
                                 List<T> results)
    {
        offset = request.startOffset;

        int lastResultIndex    = results.size();
        int lastRequestedIndex = request.maxResults > 0 ? request.maxResults + request.startOffset : Integer.MAX_VALUE;

        if (lastRequestedIndex < lastResultIndex)
        {
            nextOffset = lastRequestedIndex;
            lastResultIndex = lastRequestedIndex;
        }

        if (request.startOffset < lastResultIndex)
        {
            this.results.addAll(results.subList(request.startOffset, lastResultIndex));
        }
    }
}
