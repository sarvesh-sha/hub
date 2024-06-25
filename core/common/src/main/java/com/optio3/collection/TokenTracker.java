/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.collection;

import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;

public abstract class TokenTracker<T, P>
{
    private class Holder
    {
        final T token;
        final P payload;

        private MonotonousTime m_expiration;

        private Holder(P payload)
        {
            this.payload = payload;
            this.token = newToken(payload);

            refresh();
        }

        private void refresh()
        {
            m_expiration = TimeUtils.computeTimeoutExpiration(m_timeout, m_unit);
        }

        private void purgeIfStale()
        {
            if (TimeUtils.isTimeoutExpired(m_expiration))
            {
                close();
            }
        }

        public void close()
        {
            releasePayload(payload);

            m_tokens.remove(getTokenId(token));
        }
    }

    private final long                          m_timeout;
    private final TimeUnit                      m_unit;
    private final ConcurrentMap<String, Holder> m_tokens = Maps.newConcurrentMap();

    protected TokenTracker(long timeout,
                           TimeUnit unit)
    {
        m_timeout = timeout;
        m_unit = unit;
    }

    public List<T> list()
    {
        List<T> list = Lists.newArrayList();

        for (Holder holder : m_tokens.values())
            list.add(holder.token);

        return list;
    }

    public T register(P payload)
    {
        Holder holder = new Holder(payload);
        m_tokens.put(getTokenId(holder.token), holder);

        return holder.token;
    }

    public P get(T token)
    {
        Holder holder = token != null ? m_tokens.get(getTokenId(token)) : null;
        if (holder != null)
        {
            holder.refresh();
            return holder.payload;
        }

        return null;
    }

    public void unregister(T token)
    {
        Holder holder = token != null ? m_tokens.get(getTokenId(token)) : null;
        if (holder != null)
        {
            holder.close();
        }
    }

    public void purgeStaleEntries()
    {
        for (Holder holder : m_tokens.values())
        {
            holder.purgeIfStale();
        }
    }

    //--//

    protected abstract T newToken(P payload);

    protected abstract String getTokenId(T token);

    protected abstract void releasePayload(P payload);
}