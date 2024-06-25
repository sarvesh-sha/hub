/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.concurrency;

import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;

public class AsyncSemaphore
{
    public class Holder implements AutoCloseable
    {
        private boolean m_released;

        @Override
        public void close()
        {
            synchronized (m_mutex)
            {
                if (m_released)
                {
                    return;
                }

                m_released = true;
                m_permitsGiven--;
            }

            flushWaiters();
        }

        public int getOutstandingPermits()
        {
            return m_permitsGiven;
        }
    }

    private final Object                                m_mutex   = new Object();
    private final LinkedList<CompletableFuture<Holder>> m_waiters = new LinkedList<>();

    private final int m_permits;
    private       int m_permitsBoost;
    private       int m_permitsGiven;

    public AsyncSemaphore(int permits)
    {
        m_permits = permits;
    }

    public CompletableFuture<Holder> acquire()
    {
        CompletableFuture<Holder> res = new CompletableFuture<>();

        synchronized (m_mutex)
        {
            if (canIssuePermit())
            {
                // It's okay to resolve the future under lock, since we haven't published it yet.
                res.complete(new Holder());
                m_permitsGiven++;
            }
            else
            {
                m_waiters.add(res);
            }
        }

        return res;
    }

    public int getBoost()
    {
        return m_permitsBoost;
    }

    public void addToBoost(int boost)
    {
        synchronized (m_mutex)
        {
            m_permitsBoost += boost;

            if (m_permits + m_permitsBoost <= 0)
            {
                m_permitsBoost = 1 - m_permits;
            }
        }
    }

    public void resetBoost()
    {
        synchronized (m_mutex)
        {
            m_permitsBoost = 0;
        }
    }

    private void flushWaiters()
    {
        while (true)
        {
            CompletableFuture<Holder> nextOwner;

            synchronized (m_mutex)
            {
                if (!canIssuePermit())
                {
                    // All the permits have been assigned, we are done.
                    return;
                }

                nextOwner = m_waiters.poll();
                if (nextOwner == null)
                {
                    // No more pending waiters, we are done.
                    return;
                }

                // Assume we can give the permit to the waiter.
                m_permitsGiven++;
            }

            // Try to resolve the future outside the lock.
            if (!nextOwner.complete(new Holder()))
            {
                // The future was cancelled or already completed, we should reclaim the permit.
                synchronized (m_mutex)
                {
                    m_permitsGiven--;
                }
            }
        }
    }

    private boolean canIssuePermit()
    {
        return (m_permitsGiven < m_permits + m_permitsBoost);
    }
}
