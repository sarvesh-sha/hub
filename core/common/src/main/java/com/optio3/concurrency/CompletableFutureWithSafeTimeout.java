/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.concurrency;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.collect.Lists;

/**
 * Unfortunately, there's no way to unregister from a CompletableFuture.
 * That means that waiting on a resource that might never become available will lead to a memory leak.
 *
 * This class decouples the producer from the consumer, allowing timeouts to that don't leak memory.
 *
 * @param <T> Type of the result
 */
public class CompletableFutureWithSafeTimeout<T>
{
    private final CompletableFuture<T>       m_source;
    private final List<CompletableFuture<T>> m_awaitingResolution = Lists.newArrayList();

    public CompletableFutureWithSafeTimeout(CompletableFuture<T> source)
    {
        m_source = source;

        source.whenComplete((v, t) ->
                            {
                                List<CompletableFuture<T>> pendingLst;

                                synchronized (m_awaitingResolution)
                                {
                                    pendingLst = Lists.newArrayList(m_awaitingResolution);
                                    m_awaitingResolution.clear();
                                }

                                for (CompletableFuture<T> pending : pendingLst)
                                {
                                    if (t != null)
                                    {
                                        pending.completeExceptionally(t);
                                    }
                                    else
                                    {
                                        pending.complete(v);
                                    }
                                }
                            });
    }

    public CompletableFuture<T> waitForCompletion(long timeout,
                                                  TimeUnit unit)
    {
        CompletableFuture<T> awaitingResolution = new CompletableFuture<>();
        ScheduledFuture<?>   timer;

        synchronized (m_awaitingResolution)
        {
            if (m_source.isDone())
            {
                return m_source;
            }

            timer = Executors.scheduleOnDefaultPool(() ->
                                                    {
                                                        awaitingResolution.completeExceptionally(new TimeoutException());
                                                    }, timeout, unit);

            m_awaitingResolution.add(awaitingResolution);
        }

        awaitingResolution.whenComplete((v, t) ->
                                        {
                                            timer.cancel(false);

                                            synchronized (m_awaitingResolution)
                                            {
                                                m_awaitingResolution.remove(awaitingResolution);
                                            }
                                        });

        return awaitingResolution;
    }
}