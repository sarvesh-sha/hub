/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.asyncawait;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

import com.optio3.asyncawait.converter.KnownMethod;
import com.optio3.asyncawait.converter.KnownMethodId;

public abstract class ContinuationState extends AbstractContinuationState implements BiConsumer<Object, Throwable>,
                                                                                     Runnable
{
    public static class Foreground extends ContinuationState
    {
        @KnownMethod(KnownMethodId.ContinuationState_init_foreground)
        protected Foreground(AbstractAsyncComputation<?> context,
                             int stateId,
                             CompletableFuture<?> waitingOn)
        {
            super(context, stateId, waitingOn);
        }

        @Override
        protected void queueInner()
        {
            waitingOn.whenComplete(this);
        }

        @Override
        public String toString()
        {
            return "ContinuationState.Foreground{" + "context=" + context + '}';
        }
    }

    public static class Background extends ContinuationState
    {
        @KnownMethod(KnownMethodId.ContinuationState_init_background)
        protected Background(AbstractAsyncComputation<?> context,
                             int stateId,
                             CompletableFuture<?> waitingOn)
        {
            super(context, stateId, waitingOn);
        }

        @Override
        protected void queueInner()
        {
            waitingOn.whenCompleteAsync(this, context.getExecutor());
        }

        @Override
        public String toString()
        {
            return "ContinuationState.Background{" + "context=" + context + '}';
        }
    }

    //--//

    public final CompletableFuture<?> waitingOn;
    private      CompletableFuture<?> m_futureToUseOnRestart;

    private volatile boolean m_processed;

    protected ContinuationState(AbstractAsyncComputation<?> context,
                                int stateId,
                                CompletableFuture<?> waitingOn)
    {
        super(context, stateId);

        this.waitingOn = waitingOn;

        m_futureToUseOnRestart = waitingOn;
    }

    @KnownMethod(KnownMethodId.ContinuationState_queue)
    public void queue()
    {
        context.pendingContinuation = this;

        queueInner();

        //
        // If the external future was cancelled, pretend the future we are waiting on also got cancelled.
        //
        if (context.isDone())
        {
            cancel();
        }
    }

    @KnownMethod(KnownMethodId.ContinuationState_queueTimeout)
    public void queue(long timeout,
                      TimeUnit unit)
    {
        //
        // Both callbacks point back to us.
        // We'll use the state of m_timer to potentially cancel the future.
        //
        createTimer(this, timeout, unit);

        queue();
    }

    protected abstract void queueInner();

    @KnownMethod(KnownMethodId.ContinuationState_getFuture)
    public CompletableFuture<?> getFuture()
    {
        return m_futureToUseOnRestart;
    }

    @KnownMethod(KnownMethodId.ContinuationState_getAndUnwrapException)
    public static <T> T getAndUnwrapException(CompletableFuture<T> future) throws
                                                                           Throwable
    {
        try
        {
            return future.get();
        }
        catch (Throwable e)
        {
            if (e instanceof ExecutionException)
            {
                ExecutionException e2 = (ExecutionException) e;

                Throwable original = e2.getCause();
                if (original != null)
                {
                    throw original;
                }
            }

            throw e;
        }
    }

    @Override
    public void cancel()
    {
        if (m_futureToUseOnRestart.isDone())
        {
            // Don't recurse...
            return;
        }

        CompletableFuture<Object> cancelled = new CompletableFuture<Object>();
        cancelled.cancel(false);

        // Force the state machine to advance but in a separate thread.
        restartAsynchronously(cancelled);
    }

    //
    // Timer callback.
    //
    @Override
    public void run()
    {
        if (m_futureToUseOnRestart.isDone())
        {
            // Don't recurse...
            return;
        }

        CompletableFuture<Object> timedOut = new CompletableFuture<Object>();
        timedOut.completeExceptionally(new TimeoutException());

        // Force the state machine to advance but in a separate thread.
        restartAsynchronously(timedOut);
    }

    private void restartAsynchronously(CompletableFuture<?> future)
    {
        m_futureToUseOnRestart = future;

        future.whenCompleteAsync(this, context.getExecutor());
    }

    //
    // Future callback.
    //
    @Override
    public void accept(Object t,
                       Throwable u)
    {
        //
        // Protection against multiple completions.
        //
        synchronized (this)
        {
            if (m_processed)
            {
                return;
            }

            m_processed = true;
        }

        if (m_timer != null)
        {
            m_timer.cancel(false);
        }

        context.advance(this);
    }
}
