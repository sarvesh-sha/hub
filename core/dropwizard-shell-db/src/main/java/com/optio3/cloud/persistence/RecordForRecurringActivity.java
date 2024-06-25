/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import java.time.ZonedDateTime;
import java.util.function.Consumer;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Tuple;

import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.util.TimeUtils;

@MappedSuperclass
public abstract class RecordForRecurringActivity<T extends RecordForRecurringActivity<T>> extends RecordWithCommonFields
{
    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "next_activation", nullable = true)
    private ZonedDateTime nextActivation;

    //--//

    protected RecordForRecurringActivity()
    {
    }

    //--//

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public ZonedDateTime getNextActivation()
    {
        return nextActivation;
    }

    public void setNextActivation(ZonedDateTime nextActivation)
    {
        if (TimeUtils.compare(this.nextActivation, nextActivation) != 0)
        {
            this.nextActivation = nextActivation;
        }
    }

    //--//

    public static <R extends RecordForRecurringActivity<R>> ZonedDateTime findNextActivation(RecordHelper<R> helper,
                                                                                             Consumer<QueryHelperWithCommonFields<ZonedDateTime, R>> callback)
    {
        QueryHelperWithCommonFields<ZonedDateTime, R> jh = new QueryHelperWithCommonFields<>(helper, ZonedDateTime.class);

        jh.cq.select(jh.root.get(RecordForRecurringActivity_.nextActivation));

        refineQuery(jh, true, false, callback);

        jh.addWhereClause(jh.isNotNull(jh.root, RecordForRecurringActivity_.nextActivation));

        return jh.getFirstResultOrNull();
    }

    public static <R extends RecordForRecurringActivity<R>> TypedRecordIdentityList<R> list(RecordHelper<R> helper,
                                                                                            boolean sortByNextActivation,
                                                                                            boolean onlyReadyToGo,
                                                                                            Consumer<QueryHelperWithCommonFields<Tuple, R>> callback)
    {
        return QueryHelperWithCommonFields.list(helper, (jh) ->
        {
            refineQuery(jh, sortByNextActivation, onlyReadyToGo, callback);
        });
    }

    private static <T, R extends RecordForRecurringActivity<R>> void refineQuery(QueryHelperWithCommonFields<T, R> jh,
                                                                                 boolean sortByNextActivation,
                                                                                 boolean onlyReadyToGo,
                                                                                 Consumer<QueryHelperWithCommonFields<T, R>> callback)
    {
        if (onlyReadyToGo)
        {
            sortByNextActivation = true;

            jh.addWhereClause(jh.lessThanOrEqualTo(jh.root, RecordForRecurringActivity_.nextActivation, TimeUtils.now()));
        }

        if (sortByNextActivation)
        {
            jh.addOrderBy(jh.root, RecordForRecurringActivity_.nextActivation, true);
        }

        if (callback != null)
        {
            callback.accept(jh);
        }
    }
}
