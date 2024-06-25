/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.concurrency;

import java.util.concurrent.CompletableFuture;

public class AsyncWaitMultiple
{
    private int                     m_total;
    private int                     m_pending;
    private Throwable               m_failure;
    private CompletableFuture<Void> m_drain;

    public void add(CompletableFuture<Void> cf)
    {
        synchronized (this)
        {
            if (m_drain != null && m_drain.isDone())
            {
                m_drain = null;
            }

            m_total++;
            m_pending++;
        }

        cf.whenComplete((v, t) ->
                        {
                            synchronized (this)
                            {
                                m_pending--;

                                if (t != null && m_failure == null)
                                {
                                    m_failure = t;
                                }
                            }

                            resolveIfNeeded();
                        });
    }

    public int getTotalCount()
    {
        return m_total;
    }

    public int getPendingCount()
    {
        return m_pending;
    }

    public CompletableFuture<Void> drain()
    {
        synchronized (this)
        {
            if (m_pending == 0)
            {
                return CompletableFuture.completedFuture(null);
            }

            if (m_drain == null)
            {
                m_drain = new CompletableFuture<>();
            }

            return m_drain;
        }
    }

    private void resolveIfNeeded()
    {
        Throwable               failure;
        CompletableFuture<Void> drain;

        synchronized (this)
        {
            if (m_pending != 0)
            {
                return;
            }

            failure = m_failure;
            drain = m_drain;

            m_failure = null;
            m_drain = null;
        }

        if (failure != null)
        {
            drain.completeExceptionally(failure);
        }
        else
        {
            drain.complete(null);
        }
    }
}