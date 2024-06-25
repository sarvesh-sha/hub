/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.util;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Maps;

public class TimeUtils
{
    public static final DateTimeFormatter DEFAULT_FORMATTER_NO_MILLI = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DEFAULT_FORMATTER_MILLI    = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static final ConcurrentMap<Integer, TemporalUnit> s_truncationLookup = Maps.newConcurrentMap();

    private static final Clock  s_systemUTC     = Clock.systemUTC();
    private static       ZoneId s_systemDefault = ZoneId.systemDefault();

    //--//

    public static void setDefault(TimeZone timeZone)
    {
        TimeZone.setDefault(timeZone);
        s_systemDefault = timeZone.toZoneId();
    }

    public static long nowMilliUtc()
    {
        return s_systemUTC.millis();
    }

    public static long nowEpochSeconds()
    {
        return s_systemUTC.millis() / 1_000;
    }

    public static ZonedDateTime nowUtc()
    {
        return fromMilliToUtcTime(nowMilliUtc());
    }

    public static ZonedDateTime now()
    {
        return fromMilliToLocalTime(nowMilliUtc());
    }

    public static ZonedDateTime future(int amount,
                                       TimeUnit unit)
    {
        return fromMilliToLocalTime(nowMilliUtc() + unit.toMillis(amount));
    }

    public static ZonedDateTime past(int amount,
                                     TimeUnit unit)
    {
        return fromMilliToLocalTime(nowMilliUtc() - unit.toMillis(amount));
    }

    //--//

    public static boolean isValid(double epochSeconds)
    {
        if (Double.isNaN(epochSeconds) || epochSeconds <= Long.MIN_VALUE || epochSeconds >= Long.MAX_VALUE)
        {
            return false;
        }

        return isValid((long) epochSeconds);
    }

    public static boolean isValid(long epochSeconds)
    {
        //
        // There's a small issue.
        // We want to treat zero as a null time, since JSON would skip fields with 0.
        // That excludes midnight of 1970/1/1 as a valid time. Oh well...
        //
        return !(epochSeconds == Long.MIN_VALUE || epochSeconds == 0 || epochSeconds == Long.MAX_VALUE);
    }

    public static long minEpochSeconds()
    {
        return Long.MIN_VALUE;
    }

    public static long maxEpochSeconds()
    {
        return Long.MAX_VALUE;
    }

    public static long adjustGranularity(long time,
                                         int granularity)
    {
        return (time / granularity) * granularity;
    }

    //--//

    public static ZonedDateTime fromInstantToUtcTime(Instant instant)
    {
        return ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    public static ZonedDateTime fromInstantToLocalTime(Instant instant)
    {
        return ZonedDateTime.ofInstant(instant, s_systemDefault);
    }

    //--//

    public static ZonedDateTime fromSecondsToUtcTime(long epochSeconds)
    {
        return TimeUtils.isValid(epochSeconds) ? fromInstantToUtcTime(Instant.ofEpochSecond(epochSeconds)) : null;
    }

    public static ZonedDateTime fromSecondsToLocalTime(long epochSeconds)
    {
        return fromInstantToLocalTime(Instant.ofEpochSecond(epochSeconds));
    }

    //--//

    public static ZonedDateTime fromMilliToUtcTime(long epochMilli)
    {
        return fromInstantToUtcTime(Instant.ofEpochMilli(epochMilli));
    }

    public static ZonedDateTime fromMilliToLocalTime(long epochMilli)
    {
        return fromInstantToLocalTime(Instant.ofEpochMilli(epochMilli));
    }

    //--//

    public static ZonedDateTime fromTimestampToUtcTime(double timestamp)
    {
        if (!isValid(timestamp))
        {
            return null;
        }

        long timestampMilli = (long) (timestamp * 1000);
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestampMilli), ZoneOffset.UTC);
    }

    public static double fromUtcTimeToTimestamp(ZonedDateTime timestamp)
    {
        if (timestamp == null)
        {
            return 0.0;
        }

        return timestamp.toEpochSecond() + ((double) timestamp.getNano() / 1E9);
    }

    //--//

    public static boolean sameTimestamp(ZonedDateTime a,
                                        ZonedDateTime b)
    {
        if (a == null || b == null)
        {
            return a == b;
        }

        return a.toEpochSecond() == b.toEpochSecond() && a.getNano() == b.getNano();
    }

    public static ZonedDateTime min(ZonedDateTime a,
                                    ZonedDateTime b)
    {
        if (a == null)
        {
            return b;
        }

        if (b == null)
        {
            return a;
        }

        return a.isBefore(b) ? a : b;
    }

    public static Duration min(Duration a,
                               Duration b)
    {
        if (a == null)
        {
            return b;
        }

        if (b == null)
        {
            return a;
        }

        return a.compareTo(b) <= 0 ? a : b;
    }

    public static ZonedDateTime max(ZonedDateTime a,
                                    ZonedDateTime b)
    {
        if (a == null)
        {
            return b;
        }

        if (b == null)
        {
            return a;
        }

        return a.isAfter(b) ? a : b;
    }

    public static Duration max(Duration a,
                               Duration b)
    {
        if (a == null)
        {
            return b;
        }

        if (b == null)
        {
            return a;
        }

        return a.compareTo(b) >= 0 ? a : b;
    }

    public static Duration computeSafeDuration(long amount,
                                               TemporalUnit unit)
    {
        //
        // Special case for months, which we consider as composed of 30 days, not the average 30.436875 days.
        //
        if (unit == ChronoUnit.MONTHS)
        {
            return computeSafeDuration(amount * 30, ChronoUnit.DAYS);
        }

        //
        // Special case for years, which we consider as composed of 365 days, not the average 365.2425 days.
        //
        if (unit == ChronoUnit.YEARS)
        {
            return computeSafeDuration(amount * 365, ChronoUnit.DAYS);
        }

        return unit != null ? unit.getDuration()
                                  .multipliedBy(amount) : Duration.ZERO;
    }

    public static ZonedDateTime plusIfNotNull(ZonedDateTime a,
                                              long amount,
                                              TemporalUnit unit)
    {
        if (a != null)
        {
            a = a.plus(computeSafeDuration(amount, unit));
        }

        return a;
    }

    /**
     * Compares times, treating nulls as earlier than any other value.
     *
     * @param a Left time
     * @param b Right time
     *
     * @return the comparator value, negative if less, positive if greater
     */
    public static int compare(ZonedDateTime a,
                              ZonedDateTime b)
    {
        if (a == null)
        {
            return b == null ? 0 : -1;
        }

        if (b == null)
        {
            return 1;
        }

        //
        // ZonedDateTime.compare checks that the time zone are identical, while we care about absolute time.
        //
        int cmp = Long.compare(a.toEpochSecond(), b.toEpochSecond());
        if (cmp == 0)
        {
            cmp = Integer.compare(a.getNano(), b.getNano());
        }

        return cmp;
    }

    public static ZonedDateTime updateIfBefore(ZonedDateTime target,
                                               ZonedDateTime newValue)
    {
        return newValue != null && isAfterOrNull(target, newValue) ? newValue : target;
    }

    public static ZonedDateTime updateIfAfter(ZonedDateTime target,
                                              ZonedDateTime newValue)
    {
        return newValue != null && isBeforeOrNull(target, newValue) ? newValue : target;
    }

    public static Instant updateIfBefore(Instant target,
                                         Instant newValue)
    {
        return newValue != null && isAfterOrNull(target, newValue) ? newValue : target;
    }

    public static Instant updateIfAfter(Instant target,
                                        Instant newValue)
    {
        return newValue != null && isBeforeOrNull(target, newValue) ? newValue : target;
    }

    public static MonotonousTime updateIfBefore(MonotonousTime target,
                                                MonotonousTime newValue)
    {
        return newValue != null && isAfterOrNull(target, newValue) ? newValue : target;
    }

    public static MonotonousTime updateIfAfter(MonotonousTime target,
                                               MonotonousTime newValue)
    {
        return newValue != null && isBeforeOrNull(target, newValue) ? newValue : target;
    }

    /**
     * Checks if {@code time} is before {@code comparisonTime}.
     *
     * @param time           the date-time to compare, can be null
     * @param comparisonTime the other date-time to compare to, not null
     *
     * @return true if {@code time} is null or before {@code comparisonTime}
     */
    public static boolean isBeforeOrNull(ZonedDateTime time,
                                         ZonedDateTime comparisonTime)
    {
        return time == null || time.isBefore(comparisonTime);
    }

    /**
     * Checks if {@code time} is after {@code comparisonTime}.
     *
     * @param time           the date-time to compare, can be null
     * @param comparisonTime the other date-time to compare to, not null
     *
     * @return true if {@code time} is null or after {@code comparisonTime}
     */
    public static boolean isAfterOrNull(ZonedDateTime time,
                                        ZonedDateTime comparisonTime)
    {
        return time == null || time.isAfter(comparisonTime);
    }

    /**
     * Checks if {@code time} is before {@code comparisonTime}.
     *
     * @param time           the date-time to compare, can be null
     * @param comparisonTime the other date-time to compare to, not null
     *
     * @return true if {@code time} is null or before {@code comparisonTime}
     */
    public static boolean isBeforeOrNull(Instant time,
                                         Instant comparisonTime)
    {
        return time == null || time.isBefore(comparisonTime);
    }

    /**
     * Checks if {@code time} is after {@code comparisonTime}.
     *
     * @param time           the date-time to compare, can be null
     * @param comparisonTime the other date-time to compare to, not null
     *
     * @return true if {@code time} is null or after {@code comparisonTime}
     */
    public static boolean isAfterOrNull(Instant time,
                                        Instant comparisonTime)
    {
        return time == null || time.isAfter(comparisonTime);
    }

    /**
     * Checks if {@code time} is before {@code comparisonTime}.
     *
     * @param time           the JVM time to compare, can be null
     * @param comparisonTime the other JVM time to compare to, not null
     *
     * @return true if {@code time} is null or before {@code comparisonTime}
     */
    public static boolean isBeforeOrNull(MonotonousTime time,
                                         MonotonousTime comparisonTime)
    {
        return time == null || time.isBefore(comparisonTime);
    }

    /**
     * Checks if {@code time} is after {@code comparisonTime}.
     *
     * @param time           the JVM time to compare, can be null
     * @param comparisonTime the other JVM time to compare to, not null
     *
     * @return true if {@code time} is null or after {@code comparisonTime}
     */
    public static boolean isAfterOrNull(MonotonousTime time,
                                        MonotonousTime comparisonTime)
    {
        return time == null || time.isAfter(comparisonTime);
    }

    //--//

    public static MonotonousTime computeTimeoutExpiration(long timeout,
                                                          TimeUnit unit)
    {
        return computeTimeoutExpiration(Duration.ofNanos(unit.toNanos(timeout)));
    }

    public static MonotonousTime computeTimeoutExpiration(Duration delay)
    {
        return MonotonousTime.computeTimeoutExpiration(delay);
    }

    public static boolean isTimeoutExpired(MonotonousTime expiration)
    {
        return MonotonousTime.isTimeoutExpired(expiration);
    }

    public static boolean isTimeoutExpired(ZonedDateTime expiration)
    {
        return expiration == null || now().isAfter(expiration);
    }

    public static Duration remainingTime(MonotonousTime expiration)
    {
        if (expiration == null)
        {
            return null;
        }

        MonotonousTime now = MonotonousTime.now();

        if (now.isAfter(expiration))
        {
            return null;
        }

        return now.between(expiration);
    }

    public static boolean wasUpdatedRecently(ZonedDateTime lastUpdate,
                                             int amount,
                                             TimeUnit unit)
    {
        if (lastUpdate == null)
        {
            return false;
        }

        Instant instant = lastUpdate.toInstant();
        return wasUpdatedRecently(instant.toEpochMilli(), amount, unit);
    }

    public static boolean wasUpdatedRecently(long lastUpdateMilliUTC,
                                             int amount,
                                             TimeUnit unit)
    {
        long now = TimeUtils.nowMilliUtc();

        return lastUpdateMilliUTC + unit.toMillis(amount) >= now;
    }

    //--//

    public static boolean waitOnLock(Object lock,
                                     MonotonousTime timeoutExpiration)
    {
        try
        {
            if (timeoutExpiration == null)
            {
                lock.wait();
            }
            else
            {
                Duration timeLeft = remainingTime(timeoutExpiration);
                if (timeLeft == null)
                {
                    return false;
                }

                lock.wait(Math.max(1, timeLeft.toMillis()));
            }
        }
        catch (InterruptedException e)
        {
            // Ignore exception.
        }

        return true;
    }

    /**
     * Returns a TemporalUnit with a coarser resolution than the original one.
     *
     * @param unit        Original unit
     * @param granularity New coarseness
     *
     * @return New unit
     */
    public static TemporalUnit getModifiedUnit(TemporalUnit unit,
                                               int granularity)
    {
        return new TemporalUnit()
        {
            private final Duration m_duration = computeSafeDuration(granularity, unit);

            @Override
            public Duration getDuration()
            {
                return m_duration;
            }

            @Override
            public boolean isDurationEstimated()
            {
                return unit.isDurationEstimated();
            }

            @Override
            public boolean isDateBased()
            {
                return unit.isDateBased();
            }

            @Override
            public boolean isTimeBased()
            {
                return unit.isTimeBased();
            }

            @Override
            public <R extends Temporal> R addTo(R temporal,
                                                long amount)
            {
                return unit.addTo(temporal, amount * granularity);
            }

            @Override
            public long between(Temporal temporal1Inclusive,
                                Temporal temporal2Exclusive)
            {
                return unit.between(temporal1Inclusive, temporal2Exclusive);
            }
        };
    }

    public static ZonedDateTime truncateTimestampToMultipleOfPeriod(ZonedDateTime timestamp,
                                                                    int periodInSeconds)
    {
        TemporalUnit unit = s_truncationLookup.computeIfAbsent(periodInSeconds, (period) ->
        {
            // We use this unit only for the duration.
            return new TemporalUnit()
            {
                Duration m_value = Duration.ofSeconds(period);

                @Override
                public Duration getDuration()
                {
                    return m_value;
                }

                @Override
                public boolean isDurationEstimated()
                {
                    return false;
                }

                @Override
                public boolean isDateBased()
                {
                    return false;
                }

                @Override
                public boolean isTimeBased()
                {
                    return true;
                }

                @Override
                public <R extends Temporal> R addTo(R temporal,
                                                    long amount)
                {
                    return temporal;
                }

                @Override
                public long between(Temporal temporal1Inclusive,
                                    Temporal temporal2Exclusive)
                {
                    return 0;
                }
            };
        });

        return timestamp.truncatedTo(unit);
    }

    public static ZonedDateTime truncateToWeeks(ZonedDateTime now)
    {
        now = now.truncatedTo(ChronoUnit.DAYS);
        while (now.getDayOfWeek() != DayOfWeek.SUNDAY)
        {
            now = now.minusDays(1);
        }
        return now;
    }

    public static ZonedDateTime truncateToMonths(ZonedDateTime now)
    {
        now = now.truncatedTo(ChronoUnit.DAYS);
        return now.minusDays(now.getDayOfMonth() - 1);
    }

    public static ZonedDateTime truncateToQuarter(ZonedDateTime now)
    {
        now = truncateToMonths(now);
        return now.minusMonths((now.getMonthValue() - 1) % 3);
    }

    public static ZonedDateTime truncateToYear(ZonedDateTime now)
    {
        now = truncateToMonths(now);
        return now.minusMonths((now.getMonthValue() - 1));
    }

    public static Duration multiply(Duration duration,
                                    float scale)
    {
        float value = duration.toNanos() * scale;

        return Duration.ofNanos((long) value);
    }

    public static String toText(Duration duration)
    {
        long msec = duration.toMillis();
        if (msec < 10 * 1000)
        {
            return String.format("%,d msec", msec);
        }

        if (msec < 120 * 1000)
        {
            return String.format("%,d seconds", Math.round(msec / 1000.0));
        }

        return String.format("%,d minutes", Math.round(msec / (60 * 1000.0)));
    }
}
