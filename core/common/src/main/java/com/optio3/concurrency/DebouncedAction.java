/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.concurrency;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;

public class DebouncedAction<T>
{
    private static class State<T>
    {
        final CompletableFuture<T> computation = new CompletableFuture<>();

        ScheduledFuture<?> scheduling;

        boolean isDone()
        {
            return computation.isDone();
        }
    }

    private final Callable<CompletableFuture<T>> m_command;
    private       State<T>                       m_state;

    public DebouncedAction(Callable<CompletableFuture<T>> command)
    {
        m_command = command;
    }

    public CompletableFuture<T> schedule(MonotonousTime when)
    {
        Duration delay = TimeUtils.remainingTime(when);
        return schedule(delay != null ? delay.toMillis() : 0, TimeUnit.MILLISECONDS);
    }

    public CompletableFuture<T> schedule(long delay,
                                         TimeUnit unit)
    {
        return schedule(Executors.getDefaultScheduledExecutor(), delay, unit);
    }

    public synchronized CompletableFuture<T> schedule(ScheduledExecutorService executor,
                                                      long delay,
                                                      TimeUnit unit)
    {
        if (m_state != null && m_state.isDone())
        {
            m_state = null;
        }

        if (m_state == null)
        {
            State<T> state = new State<>();

            state.scheduling = executor.schedule(() ->
                                                 {
                                                     if (shouldProceed(state))
                                                     {
                                                         try
                                                         {
                                                             CompletableFuture<T> futureVal = m_command.call();
                                                             futureVal.whenComplete((val, t) ->
                                                                                    {
                                                                                        if (t != null)
                                                                                        {
                                                                                            state.computation.completeExceptionally(t);
                                                                                        }
                                                                                        else
                                                                                        {
                                                                                            state.computation.complete(val);
                                                                                        }
                                                                                    });
                                                         }
                                                         catch (Throwable t1)
                                                         {
                                                             state.computation.completeExceptionally(t1);
                                                         }
                                                     }
                                                     else
                                                     {
                                                         state.computation.cancel(false);
                                                     }
                                                 }, delay, unit);

            m_state = state;
        }

        return m_state.computation;
    }

    public synchronized boolean isScheduled()
    {
        State<T> state = m_state;
        return state != null && state.scheduling != null;
    }

    public synchronized void cancel()
    {
        State<T> state = m_state;
        if (state != null)
        {
            if (state.scheduling != null)
            {
                state.scheduling.cancel(false);
                state.scheduling = null;

                m_state = null;
            }
        }
    }

    private synchronized boolean shouldProceed(State<T> state)
    {
        if (m_state == state && state.scheduling != null)
        {
            state.scheduling = null;
            return true;
        }

        return false;
    }
}