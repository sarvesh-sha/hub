/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;

import com.optio3.util.CollectionUtils;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.query.Query;

public abstract class AbstractSelectHelper<T, R> extends AbstractQueryHelper<T, R>
{
    public class ScrollHolder<R> implements Closeable
    {
        private final ScrollableResults m_sr;

        public ScrollHolder(ScrollableResults sr)
        {
            m_sr = sr;
        }

        @Override
        public void close() throws
                            IOException
        {
            m_sr.close();

            cleanupAfterQuery();
        }

        public boolean next()
        {
            return m_sr.next();
        }

        public R get(int i)
        {
            @SuppressWarnings("unchecked") R entity = (R) m_sr.get(i);

            return SessionHolder.unwrapProxy(entity);
        }
    }

    public final CriteriaQuery<T> cq;

    private Integer m_fetchSize;

    protected AbstractSelectHelper(RecordHelper<R> helper,
                                   Class<T> clz)
    {
        super(helper);

        cq = cb.createQuery(clz);
    }

    private Query<T> createQuery(int maxResults)
    {
        Predicate[] arrayWhere = getWherePredicates();
        if (arrayWhere != null)
        {
            cq.where(arrayWhere);
        }

        Order[] arrayOrder = getOrderPredicates();
        if (arrayOrder != null)
        {
            cq.orderBy(arrayOrder);
        }

        Query<T> q = helper.currentSessionHolder()
                           .createQuery(cq);

        if (m_fetchSize != null)
        {
            q.setFetchSize(m_fetchSize);
        }

        if (maxResults > 0)
        {
            q.setMaxResults(maxResults);
        }

        return q;
    }

    public void setFetchSize(int fetchSize)
    {
        m_fetchSize = fetchSize;
    }

    //--//

    public ScrollHolder<T> scroll()
    {
        return scroll(0);
    }

    public ScrollHolder<T> scroll(int maxResults)
    {
        // When scrolling, make sure we have a limit to the fetch size.
        if (m_fetchSize == null)
        {
            m_fetchSize = 200;
        }

        Query<T> query = createQuery(maxResults);
        return new ScrollHolder<>(query.scroll(ScrollMode.FORWARD_ONLY));
    }

    public List<T> list()
    {
        return list(0);
    }

    public List<T> list(int maxResults)
    {
        Query<T> query   = createQuery(maxResults);
        List<T>  results = SessionHolder.unwrapProxies(query.getResultList());

        cleanupAfterQuery();

        return results;
    }

    public T getSingleResult()
    {
        Query<T> query  = createQuery(1);
        T        result = SessionHolder.unwrapProxy(query.getSingleResult());

        cleanupAfterQuery();

        return result;
    }

    public T getFirstResultOrNull()
    {
        List<T> list = list(1);

        return CollectionUtils.firstElement(list);
    }
}
