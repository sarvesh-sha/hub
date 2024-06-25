/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import java.util.List;

import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;

import org.hibernate.query.Query;

public abstract class AbstractDeleteHelper<T> extends AbstractQueryHelper<T, T>
{
    public final CriteriaDelete<T> cq;

    private List<Order> m_orders;

    protected AbstractDeleteHelper(RecordHelper<T> helper)
    {
        super(helper);

        cq = cb.createCriteriaDelete(helper.getEntityClass());
    }

    private Query<T> createQuery()
    {
        Predicate[] array = getWherePredicates();
        if (array != null)
        {
            cq.where(array);
        }

        return helper.currentSessionHolder()
                     .createQuery(cq);
    }

    //--//

    public int execute()
    {
        helper.currentSessionHolder()
              .checkInTransaction();

        return createQuery().executeUpdate();
    }
}
