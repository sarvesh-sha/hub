/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.concurrency;

import java.util.concurrent.TimeUnit;

public class RateLimiter
{
    public class Holder implements AutoCloseable
    {
        final Object lock = new Object();
        final int    minAvailablePermits;

        Holder  succ;
        Holder  prev;
        boolean acquired;

        public Holder(int minAvailablePermits)
        {
            this.minAvailablePermits = minAvailablePermits;
        }

        @Override
        public void close()
        {
            synchronized (m_head)
            {
                if (wasAcquired())
                {
                    markAsReleased();

                    assignPermitsToWaitingThreads();
                }
            }
        }

        public boolean wasAcquired()
        {
            return acquired;
        }

        void markAsAcquired()
        {
            m_consumedPermits++;
            acquired = true;
        }

        void markAsReleased()
        {
            acquired          = false;
            m_consumedPermits = Math.max(0, m_consumedPermits - 1);
        }

        void queue()
        {
            Holder succ = m_tail;
            Holder prev = succ.prev;

            this.succ = succ;
            this.prev = prev;

            succ.prev = this;
            prev.succ = this;
            m_queueDepth++;
        }

        void dequeue()
        {
            Holder succ = this.succ;
            Holder prev = this.prev;

            if (succ != null)
            {
                succ.prev = prev;
                prev.succ = succ;
                m_queueDepth--;

                this.succ = null;
                this.prev = null;
            }
        }
    }

    private final Holder m_head;
    private final Holder m_tail;
    private final int    m_maxPermits;
    private       int    m_consumedPermits;
    private       int    m_queueDepth;

    public RateLimiter(int maxPermits)
    {
        m_maxPermits = maxPermits;

        m_head = new Holder(-1);
        m_tail = new Holder(-1);

        m_head.succ = m_tail;
        m_tail.prev = m_head;
    }

    public int getMaxPermits()
    {
        return m_maxPermits;
    }

    public int getConsumedPermits()
    {
        return m_consumedPermits;
    }

    public int getQueueDepth()
    {
        return m_queueDepth;
    }

    public Holder acquire(int minAvailablePermits,
                          int wait,
                          TimeUnit unit)
    {
        Holder holder = new Holder(minAvailablePermits);

        synchronized (m_head)
        {
            holder.queue();

            assignPermitsToWaitingThreads();
        }

        synchronized (holder.lock)
        {
            if (!holder.acquired)
            {
                try
                {
                    holder.lock.wait(unit.toMillis(wait));
                }
                catch (InterruptedException e)
                {
                    // Assume failed to acquire if interrupted.
                }
            }
        }

        synchronized (m_head)
        {
            holder.dequeue();
        }

        return holder;
    }

    private void assignPermitsToWaitingThreads()
    {
        for (Holder holder = m_head.succ; holder != m_tail; )
        {
            Holder succ = holder.succ;

            synchronized (holder.lock)
            {
                if (holder.minAvailablePermits == 0 || holder.minAvailablePermits <= m_maxPermits - m_consumedPermits)
                {
                    holder.dequeue();
                    holder.markAsAcquired();
                    holder.lock.notify();
                }
            }

            holder = succ;
        }
    }
}
