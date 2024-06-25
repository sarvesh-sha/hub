/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import java.util.function.Consumer;

import javax.persistence.MappedSuperclass;
import javax.persistence.metamodel.SingularAttribute;

@MappedSuperclass
public abstract class RecordWithSequenceNumber<T extends RecordWithSequenceNumber> extends RecordWithCommonFields
{
    protected int assignUniqueNumber(RecordHelper<? extends T> helper,
                                     Integer number,
                                     Consumer<QueryHelperWithCommonFields<? extends T, ? extends T>> callback)
    {
        if (number == null)
        {
            number = findUniqueNumber(helper, callback);
        }

        return number;
    }

    private int findUniqueNumber(RecordHelper<? extends T> helper,
                                 Consumer<QueryHelperWithCommonFields<? extends T, ? extends T>> callback)
    {
        T rec_top = QueryHelperWithCommonFields.getFirstMatch(helper, (jh) ->
        {
            jh.addOrderBy(jh.root, fetchSequenceNumberField(), false);

            if (callback != null)
            {
                callback.accept(jh);
            }
        });

        return rec_top != null ? rec_top.fetchSequenceNumberValue() + 1 : 1;
    }

    protected abstract SingularAttribute<T, Integer> fetchSequenceNumberField();

    protected abstract int fetchSequenceNumberValue();
}