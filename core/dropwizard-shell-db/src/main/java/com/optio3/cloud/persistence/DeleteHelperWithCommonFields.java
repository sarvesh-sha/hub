/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import javax.persistence.criteria.Root;

public class DeleteHelperWithCommonFields<R extends RecordWithCommonFields> extends AbstractDeleteHelper<R>
{
    public final Root<R> root;

    public DeleteHelperWithCommonFields(RecordHelper<R> helper)
    {
        super(helper);

        root = cq.from(helper.getEntityClass());
    }
}