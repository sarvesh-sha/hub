/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.concurrency;

import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;

public class AsyncMutex
{
    public class Holder implements AutoCloseable
    {
        @Override
        public void close()
        {
            synchronized (m_mutex)
            {
                if (m_owned == this) // Make sure we still own the mutex...
                {
                    CompletableFuture<Holder> nextOwner;

                    while ((nextOwner = m_waiters.poll()) != null)
                    {
                        if (nextOwner.complete(this))
                        {
                            // The future was not cancelled or anything, we are good.
                            return;
                        }
                    }

                    // Nobody waiting, release mutex.
                    m_owned = null;
                }
            }
        }
    }

    private final Object                                m_mutex   = new Object();
    private final LinkedList<CompletableFuture<Holder>> m_waiters = new LinkedList<>();
    private       Holder                                m_owned;

    public CompletableFuture<Holder> acquire()
    {
        CompletableFuture<Holder> res = new CompletableFuture<Holder>();

        synchronized (m_mutex)
        {
            if (m_owned != null)
            {
                m_waiters.add(res);
            }
            else
            {
                m_owned = new Holder();
                res.complete(m_owned);
            }
        }

        return res;
    }
}
