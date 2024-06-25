/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.logic;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.Sets;
import com.optio3.cloud.AbstractApplicationWithDatabase;
import com.optio3.cloud.persistence.DbEvent;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.util.CollectionUtils;
import com.optio3.util.function.ConsumerWithException;
import com.optio3.util.function.FunctionWithException;

public abstract class RecurringActivityHandler
{
    public interface ForTable
    {
        Class<?> getEntityClass();

        boolean shouldTrigger(DbEvent event);
    }

    //--//

    private Set<String> m_pending;

    protected void trackPending(String sysId)
    {
        synchronized (this)
        {
            if (m_pending == null)
            {
                m_pending = Sets.newHashSet();
            }

            m_pending.add(sysId);
        }
    }

    protected Set<String> flushPending()
    {
        Set<String> pending;

        synchronized (this)
        {
            pending = m_pending;
            m_pending = null;
        }

        return pending;
    }

    //--//

    public abstract Duration startupDelay();

    public abstract CompletableFuture<ZonedDateTime> process(SessionProvider sessionProvider) throws
                                                                                              Exception;

    public abstract void shutdown();

    //--//

    public <I, O> List<O> transformInParallel(Collection<I> coll,
                                              FunctionWithException<I, O> transform)
    {
        return CollectionUtils.transformInParallel(coll, AbstractApplicationWithDatabase.GlobalRateLimiter, transform);
    }

    public <I> void callInParallel(Collection<I> coll,
                                   ConsumerWithException<I> transform)
    {
        CollectionUtils.transformInParallel(coll, AbstractApplicationWithDatabase.GlobalRateLimiter, (val) ->
        {
            transform.accept(val);
            return null;
        });
    }
}
