/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet;

import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import com.optio3.util.Exceptions;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import com.optio3.util.concurrency.DeadlineTimeoutException;

public class TimeoutSpec
{
    public final int            retries;
    public final Duration       timeout;
    public final MonotonousTime deadline;
    public final Exception      deadlineException;

    private TimeoutSpec(int retries,
                        Duration timeout,
                        MonotonousTime deadline,
                        Exception deadlineException)
    {
        this.retries           = retries;
        this.timeout           = requireNonNull(timeout);
        this.deadline          = deadline;
        this.deadlineException = deadlineException;
    }

    public static TimeoutSpec create(int retries,
                                     Duration timeout)
    {
        return new TimeoutSpec(retries, timeout, null, null);
    }

    public TimeoutSpec withRetries(int retries)
    {
        return new TimeoutSpec(retries, timeout, deadline, deadlineException);
    }

    public TimeoutSpec withTimeout(long timeout,
                                   ChronoUnit unit)
    {
        return new TimeoutSpec(retries, TimeUtils.computeSafeDuration(timeout, unit), deadline, deadlineException);
    }

    public TimeoutSpec withDeadline(MonotonousTime deadline)
    {
        return new TimeoutSpec(retries, timeout, deadline, Exceptions.newGenericException(DeadlineTimeoutException.class, "Timeout past deadline of %s", deadline));
    }

    public boolean isPastDeadline()
    {
        return deadline != null && TimeUtils.remainingTime(deadline) == null;
    }

    public Duration capToDeadline(Duration timeout)
    {
        if (deadline != null)
        {
            Duration timeLeft = TimeUtils.remainingTime(deadline);
            if (timeLeft == null)
            {
                return null;
            }

            return TimeUtils.min(timeout, timeLeft);
        }

        return timeout;
    }
}
