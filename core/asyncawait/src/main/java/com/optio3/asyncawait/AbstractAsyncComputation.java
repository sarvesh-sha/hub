/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.asyncawait;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.optio3.asyncawait.converter.KnownMethod;
import com.optio3.asyncawait.converter.KnownMethodId;
import com.optio3.concurrency.Executors;

public abstract class AbstractAsyncComputation<T> extends CompletableFuture<T>
{
    /**
     * A CompletableFuture that resolves after a certain delay.
     */
    static public class Delayed extends CompletableFuture<Void> implements Runnable
    {
        private final ScheduledFuture<?> m_timer;

        Delayed(AbstractAsyncComputation<?> comp,
                long delay,
                TimeUnit unit)
        {
            m_timer = comp.getScheduledExecutor()
                          .schedule(this, delay, unit);
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning)
        {
            m_timer.cancel(mayInterruptIfRunning);

            return super.cancel(mayInterruptIfRunning);
        }

        @Override
        public void run()
        {
            this.complete(null);
        }
    }

    private final ThreadPoolExecutor       m_threadPoolExecutor;
    private final ScheduledExecutorService m_scheduledExecutor;

    protected AbstractContinuationState pendingContinuation;

    @KnownMethod(KnownMethodId.AsyncComputation_init)
    protected AbstractAsyncComputation(ThreadPoolExecutor threadPoolExecutor,
                                       ScheduledExecutorService scheduledExecutor)
    {
        m_threadPoolExecutor = threadPoolExecutor;
        m_scheduledExecutor = scheduledExecutor;
    }

    public ThreadPoolExecutor getExecutor()
    {
        ThreadPoolExecutor executor = m_threadPoolExecutor;

        if (executor == null)
        {
            executor = Executors.getDefaultThreadPool();
        }

        return executor;
    }

    public ScheduledExecutorService getScheduledExecutor()
    {
        ScheduledExecutorService executor = m_scheduledExecutor;

        if (executor == null)
        {
            executor = Executors.getDefaultScheduledExecutor();
        }

        return executor;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning)
    {
        AbstractContinuationState cs = pendingContinuation;
        if (cs != null)
        {
            cs.cancel();
        }

        return super.cancel(mayInterruptIfRunning);
    }

    @KnownMethod(KnownMethodId.AsyncComputation_start)
    public final void start()
    {
        advance(null);
    }

    @KnownMethod(KnownMethodId.AsyncComputation_startDelayed)
    public final void startDelayed(long timeout,
                                   TimeUnit unit)
    {
        DelayedStartContinuationState cs = new DelayedStartContinuationState(this);

        cs.queue(timeout, unit);
    }

    @KnownMethod(KnownMethodId.AsyncComputation_sleep)
    public static CompletableFuture<Void> sleep(long delay,
                                                TimeUnit unit,
                                                AbstractAsyncComputation<?> pThis) throws
                                                                                   Exception
    {
        return new Delayed(pThis, delay, unit);
    }

    final void advance(AbstractContinuationState state)
    {
        try
        {
            pendingContinuation = null;

            int stateId;

            if (state != null)
            {
                stateId = state.stateId;

                //
                // Abandon processing if we were externally completed.
                //
                if (isDone())
                {
                    state.cancel();
                }
            }
            else
            {
                stateId = 0;
            }

            advanceInner(stateId, state);
        }
        catch (Throwable ex)
        {
            completeExceptionally(unwrap(ex));
        }
    }

    public static Throwable unwrap(Throwable ex)
    {
        if (ex instanceof ExecutionException)
        {
            ExecutionException ee = (ExecutionException) ex;
            ex = ee.getCause();
        }

        return ex;
    }

    @KnownMethod(KnownMethodId.AsyncComputation_advanceInner)
    protected abstract void advanceInner(int stateId,
                                         AbstractContinuationState state);

    @KnownMethod(KnownMethodId.AsyncComputation_invalidState)
    protected RuntimeException invalidState(int stateId)
    {
        return new RuntimeException("INTERNAL ERROR: Async State Machine has received unknown state " + stateId);
    }

    @KnownMethod(KnownMethodId.AsyncComputation_forwardFuture)
    public void forwardFuture(CompletableFuture<T> result)
    {
        result.whenComplete((v, ex) ->
                            {
                                if (ex != null)
                                {
                                    completeExceptionally(ex);
                                }
                                else
                                {
                                    complete(v);
                                }
                            });
    }
}
