/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import static java.util.Objects.requireNonNull;

public abstract class AbstractQueryHelper<T, R> extends AbstractQueryHelperBase
{
    public final RecordHelper<R> helper;

    protected AbstractQueryHelper(RecordHelper<R> helper)
    {
        super(requireNonNull(helper).currentSessionHolder()
                                    .getCriteriaBuilder());

        this.helper = helper;
    }

    @Override
    protected void cleanupAfterQuery()
    {
        cleanupAfterQuery(helper.currentSessionHolder());
    }
}
