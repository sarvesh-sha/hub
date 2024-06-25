/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.concurrency;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * This class allows asynchronous consumers to block if the gate is closed.
 * <br>
 * A producer will use {@link #blockConsumers()} in a try-resource pattern to close the gate.
 */
public class AsyncGate
{
    public class Holder implements AutoCloseable
    {
        private boolean m_acquired;

        private Holder()
        {
            m_acquired = true;

            synchronized (m_lock)
            {
                flagAsClosed();

                if (m_delayedOpening != null)
                {
                    m_delayedOpening.cancel(false);
                    m_delayedOpening = null;
                }

                m_blockCount++;
            }
        }

        @Override
        public void close()
        {
            synchronized (m_lock)
            {
                if (!m_acquired)
                {
                    throw new IllegalStateException("Gate not acquired");
                }

                m_acquired = false;
                m_blockCount--;

                if (m_blockCount == 0)
                {
                    m_delayedOpening = Executors.scheduleOnDefaultPool(AsyncGate.this::delayedOpening, m_delay, m_unit);
                }
            }
        }
    }

    private static final CompletableFuture<Void> s_gateOpen = CompletableFuture.completedFuture(null);

    private final long                    m_delay;
    private final TimeUnit                m_unit;
    private final Object                  m_lock;
    private       int                     m_blockCount;
    private       CompletableFuture<Void> m_gateClosed;
    private       ScheduledFuture<?>      m_delayedOpening;

    public AsyncGate(long delay,
                     TimeUnit unit)
    {
        m_delay = delay;
        m_unit = unit;
        m_lock = new Object();
    }

    public Holder blockConsumers()
    {
        return new Holder();
    }

    public CompletableFuture<Void> getWaiter()
    {
        synchronized (m_lock)
        {
            if (m_gateClosed != null)
            {
                return m_gateClosed;
            }

            return s_gateOpen;
        }
    }

    private void delayedOpening()
    {
        CompletableFuture<Void> gateClosed;

        synchronized (m_lock)
        {
            m_delayedOpening = null;

            if (m_blockCount == 0)
            {
                gateClosed = m_gateClosed;
                m_gateClosed = null;
            }
            else
            {
                gateClosed = null;
            }
        }

        if (gateClosed != null)
        {
            gateClosed.complete(null);
        }
    }

    private void flagAsClosed()
    {
        if (m_gateClosed == null)
        {
            m_gateClosed = new CompletableFuture<>();
        }
    }
}
