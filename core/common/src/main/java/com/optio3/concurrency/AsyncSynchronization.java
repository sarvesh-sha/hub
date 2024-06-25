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
 * This class allows asynchronous consumers to synchronized on an event.
 * <br>
 * A producer will use {@link #signal()} to wake up all the waiters.
 * <br>
 * If a consumer enters an already-signaled synchronization, it will reset the condition before returning.
 */
public class AsyncSynchronization
{
    private final Object                  m_lock;
    private       CompletableFuture<Void> m_signal;

    public AsyncSynchronization()
    {
        m_lock = new Object();
        m_signal = new CompletableFuture<>();
    }

    public void signal()
    {
        synchronized (m_lock)
        {
            m_signal.complete(null);
        }
    }

    public CompletableFuture<Void> condition()
    {
        synchronized (m_lock)
        {
            CompletableFuture<Void> signal = m_signal;

            if (m_signal.isDone())
            {
                m_signal = new CompletableFuture<>();
            }

            return signal;
        }
    }

    public CompletableFuture<Boolean> condition(int timeout,
                                                TimeUnit unit)
    {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        CompletableFuture<Void>    signal = condition();

        if (signal.isDone())
        {
            result.complete(true);
        }
        else
        {
            ScheduledFuture<Object> wakeup = Executors.scheduleOnDefaultPool(() -> result.complete(false), timeout, unit);

            signal.whenComplete((res, t) ->
                                {
                                    wakeup.cancel(false);
                                    result.complete(true);
                                });
        }

        return result;
    }
}
