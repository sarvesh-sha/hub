/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.util;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class MonotonousTime implements Comparable<MonotonousTime>
{
    private final long m_milliSec;

    private MonotonousTime(long milliSec)
    {
        m_milliSec = milliSec;
    }

    //--//

    @JsonCreator
    public static MonotonousTime fromJson(Instant instant)
    {
        return fromInstant(instant);
    }

    @JsonValue
    public Instant toJson()
    {
        return toInstant(this);
    }

    private static MonotonousTime fromInstant(Instant val)
    {
        if (val == null)
        {
            return null;
        }

        Instant        now1 = Instant.now();
        MonotonousTime now2 = MonotonousTime.now();

        Duration diff = Duration.between(now1, val);
        return now2.plus(diff);
    }

    private static Instant toInstant(MonotonousTime val)
    {
        Instant        now1 = Instant.now();
        MonotonousTime now2 = MonotonousTime.now();

        Duration diff = Duration.ofMillis(val.m_milliSec - now2.m_milliSec);
        return now1.plus(diff);
    }

    @Override
    public String toString()
    {
        return toInstant(this).toString();
    }

    //--//

    private static long getMilliSec()
    {
        final long nanoSec = System.nanoTime();
        return nanoSec / 1_000_000;
    }

    public static MonotonousTime now()
    {
        return new MonotonousTime(getMilliSec());
    }

    public static MonotonousTime computeTimeoutExpiration(Duration delay)
    {
        return new MonotonousTime(getMilliSec() + delay.toMillis());
    }

    public static boolean isTimeoutExpired(MonotonousTime expiration)
    {
        if (expiration == null)
        {
            return true;
        }

        return getMilliSec() > expiration.m_milliSec;
    }

    public MonotonousTime plus(long amount,
                               ChronoUnit unit)
    {
        return plus(TimeUtils.computeSafeDuration(amount, unit));
    }

    public MonotonousTime plus(Duration duration)
    {
        return new MonotonousTime(m_milliSec + duration.toMillis());
    }

    public MonotonousTime minus(long amount,
                                ChronoUnit unit)
    {
        return minus(TimeUtils.computeSafeDuration(amount, unit));
    }

    public MonotonousTime minus(Duration duration)
    {
        return new MonotonousTime(m_milliSec - duration.toMillis());
    }

    @Override
    public int compareTo(MonotonousTime other)
    {
        return Long.compare(m_milliSec, other.m_milliSec);
    }

    public boolean isAfter(MonotonousTime other)
    {
        return compareTo(other) > 0;
    }

    public boolean isBefore(MonotonousTime other)
    {
        return compareTo(other) < 0;
    }

    public Duration between(MonotonousTime end)
    {
        return Duration.ofMillis(end.m_milliSec - m_milliSec);
    }
}
