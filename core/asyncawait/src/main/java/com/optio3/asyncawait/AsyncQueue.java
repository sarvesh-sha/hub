/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.asyncawait;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

public class AsyncQueue<T>
{
    private final Queue<T> m_pending = new LinkedList<>();

    private CompletableFuture<T> m_nextFutureValue;

    //--//

    public int size()
    {
        synchronized (m_pending)
        {
            return m_pending.size();
        }
    }

    public CompletableFuture<T> pull()
    {
        synchronized (m_pending)
        {
            if (m_nextFutureValue == null)
            {
                //
                // If no previous consumers and list is not empty, return value immediately.
                //
                if (!m_pending.isEmpty())
                {
                    T value = m_pending.poll();

                    return CompletableFuture.completedFuture(value);
                }

                //
                // Otherwise prepare a completable, which means the next value pushed is already designated to be routed this way.
                //
                m_nextFutureValue = new CompletableFuture<>();
            }

            return m_nextFutureValue;
        }
    }

    public void push(T value)
    {
        CompletableFuture<T> pendingFuture;

        complete:
        synchronized (m_pending)
        {
            if (m_pending.isEmpty())
            {
                if (m_nextFutureValue != null)
                {
                    //
                    // The value was already promised to a consumer, route it directly to it.
                    //
                    pendingFuture = m_nextFutureValue;
                    m_nextFutureValue = null;
                    break complete;
                }
            }

            m_pending.add(value);
            return;
        }

        pendingFuture.complete(value);
    }
}
