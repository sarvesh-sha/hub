/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.asyncawait;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class AbstractContinuationState
{
    public final AbstractAsyncComputation<?> context;
    public final int                         stateId;

    protected ScheduledFuture<?> m_timer;

    protected AbstractContinuationState(AbstractAsyncComputation<?> context,
                                        int stateId)
    {
        this.context = context;
        this.stateId = stateId;
    }

    public abstract void cancel();

    protected void createTimer(Runnable target,
                               long timeout,
                               TimeUnit unit)
    {
        m_timer = context.getScheduledExecutor()
                         .schedule(target, timeout, unit != null ? unit : TimeUnit.SECONDS);
    }
}
