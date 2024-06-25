/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.asyncawait;

import java.util.concurrent.TimeUnit;

class DelayedStartContinuationState extends AbstractContinuationState implements Runnable
{
    DelayedStartContinuationState(AbstractAsyncComputation<?> context)
    {
        super(context, 0);
    }

    public void queue(long timeout,
                      TimeUnit unit)
    {
        createTimer(this, timeout, unit);

        context.pendingContinuation = this;
    }

    @Override
    public void cancel()
    {
        if (m_timer.isDone())
        {
            // Don't recurse...
            return;
        }

        m_timer.cancel(false);
    }

    //
    // Timer callback.
    //
    @Override
    public void run()
    {
        context.start();
    }

    @Override
    public String toString()
    {
        return "DelayedStartContinuationState{" + "context=" + context + '}';
    }
}
