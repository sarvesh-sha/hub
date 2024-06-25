/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.jobs;

import java.time.ZonedDateTime;
import java.util.List;

import com.optio3.cloud.model.SortCriteria;
import com.optio3.util.CollectionUtils;

public class JobFilterRequest
{
    public ZonedDateTime after;

    public ZonedDateTime before;

    public boolean executing;

    public List<SortCriteria> sortBy;

    //--//

    public boolean hasSorting()
    {
        return hasItems(sortBy);
    }

    private static boolean hasItems(List<?> coll)
    {
        return CollectionUtils.isNotEmpty(coll);
    }
}
