/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.visualization;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import com.optio3.util.TimeUtils;

public enum TimeRangeId
{
    Last15Minutes
            {
                @Override
                protected void compute(TimeRange result,
                                       ZonedDateTime now,
                                       boolean alignToBoundary)
                {
                    if (alignToBoundary)
                    {
                        now = now.truncatedTo(ChronoUnit.MINUTES);
                    }

                    result.start = now.minusMinutes(15);
                    result.end   = now;
                }
            },
    Last30Minutes
            {
                @Override
                protected void compute(TimeRange result,
                                       ZonedDateTime now,
                                       boolean alignToBoundary)
                {
                    if (alignToBoundary)
                    {
                        now = now.truncatedTo(ChronoUnit.MINUTES);
                    }

                    result.start = now.minusMinutes(30);
                    result.end   = now;
                }
            },
    Last60Minutes
            {
                @Override
                protected void compute(TimeRange result,
                                       ZonedDateTime now,
                                       boolean alignToBoundary)
                {
                    if (alignToBoundary)
                    {
                        now = now.truncatedTo(ChronoUnit.MINUTES);
                    }

                    result.start = now.minusMinutes(60);
                    result.end   = now;
                }
            },
    Hour
            {
                @Override
                protected void compute(TimeRange result,
                                       ZonedDateTime now,
                                       boolean alignToBoundary)
                {
                    now = now.truncatedTo(ChronoUnit.HOURS);

                    result.start = now;
                    result.end   = now.plusHours(1);
                }
            },
    PreviousHour
            {
                @Override
                protected void compute(TimeRange result,
                                       ZonedDateTime now,
                                       boolean alignToBoundary)
                {
                    now = now.truncatedTo(ChronoUnit.HOURS);

                    result.start = now.minusHours(1);
                    result.end   = now;
                }
            },
    Last3Hours
            {
                @Override
                protected void compute(TimeRange result,
                                       ZonedDateTime now,
                                       boolean alignToBoundary)
                {
                    if (alignToBoundary)
                    {
                        now = now.truncatedTo(ChronoUnit.HOURS);
                    }

                    result.start = now.minusHours(3);
                    result.end   = now;
                }
            },
    Last6Hours
            {
                @Override
                protected void compute(TimeRange result,
                                       ZonedDateTime now,
                                       boolean alignToBoundary)
                {
                    if (alignToBoundary)
                    {
                        now = now.truncatedTo(ChronoUnit.HOURS);
                    }

                    result.start = now.minusHours(6);
                    result.end   = now;
                }
            },
    Last12Hours
            {
                @Override
                protected void compute(TimeRange result,
                                       ZonedDateTime now,
                                       boolean alignToBoundary)
                {
                    if (alignToBoundary)
                    {
                        now = now.truncatedTo(ChronoUnit.HOURS);
                    }

                    result.start = now.minusHours(12);
                    result.end   = now;
                }
            },
    Last24Hours
            {
                @Override
                protected void compute(TimeRange result,
                                       ZonedDateTime now,
                                       boolean alignToBoundary)
                {
                    if (alignToBoundary)
                    {
                        now = now.truncatedTo(ChronoUnit.HOURS);
                    }

                    result.start = now.minusHours(24);
                    result.end   = now;
                }
            },
    Today
            {
                @Override
                protected void compute(TimeRange result,
                                       ZonedDateTime now,
                                       boolean alignToBoundary)
                {
                    now = now.truncatedTo(ChronoUnit.DAYS);

                    result.start = now;
                    result.end   = now.plusDays(1);
                }
            },
    Yesterday
            {
                @Override
                protected void compute(TimeRange result,
                                       ZonedDateTime now,
                                       boolean alignToBoundary)
                {
                    now = now.truncatedTo(ChronoUnit.DAYS);

                    result.start = now.minusDays(1);
                    result.end   = now;
                }
            },
    Last2Days
            {
                @Override
                protected void compute(TimeRange result,
                                       ZonedDateTime now,
                                       boolean alignToBoundary)
                {
                    if (alignToBoundary)
                    {
                        now = now.truncatedTo(ChronoUnit.DAYS);
                    }

                    result.start = now.minusDays(2);
                    result.end   = now;
                }
            },
    Last3Days
            {
                @Override
                protected void compute(TimeRange result,
                                       ZonedDateTime now,
                                       boolean alignToBoundary)
                {
                    if (alignToBoundary)
                    {
                        now = now.truncatedTo(ChronoUnit.DAYS);
                    }

                    result.start = now.minusDays(3);
                    result.end   = now;
                }
            },
    Last7Days
            {
                @Override
                protected void compute(TimeRange result,
                                       ZonedDateTime now,
                                       boolean alignToBoundary)
                {
                    if (alignToBoundary)
                    {
                        now = now.truncatedTo(ChronoUnit.DAYS);
                    }

                    result.start = now.minusDays(7);
                    result.end   = now;
                }
            },
    Week
            {
                @Override
                protected void compute(TimeRange result,
                                       ZonedDateTime now,
                                       boolean alignToBoundary)
                {
                    if (alignToBoundary)
                    {
                        now = now.truncatedTo(ChronoUnit.DAYS);
                    }

                    now = TimeUtils.truncateToWeeks(now);

                    result.start = now;
                    result.end   = now.plusWeeks(1);
                }
            },
    PreviousWeek
            {
                @Override
                protected void compute(TimeRange result,
                                       ZonedDateTime now,
                                       boolean alignToBoundary)
                {
                    now = TimeUtils.truncateToWeeks(now);

                    result.start = now.minusWeeks(1);
                    result.end   = now;
                }
            },
    Month
            {
                @Override
                protected void compute(TimeRange result,
                                       ZonedDateTime now,
                                       boolean alignToBoundary)
                {
                    now = TimeUtils.truncateToMonths(now);

                    result.start = now;
                    result.end   = now.plusMonths(1);
                }
            },
    PreviousMonth
            {
                @Override
                protected void compute(TimeRange result,
                                       ZonedDateTime now,
                                       boolean alignToBoundary)
                {
                    now = TimeUtils.truncateToMonths(now);

                    result.start = now.minusMonths(1);
                    result.end   = now;
                }
            },
    Last30Days
            {
                @Override
                protected void compute(TimeRange result,
                                       ZonedDateTime now,
                                       boolean alignToBoundary)
                {
                    if (alignToBoundary)
                    {
                        now = now.truncatedTo(ChronoUnit.DAYS);
                    }

                    result.start = now.minusDays(30);
                    result.end   = now;
                }
            },
    Quarter
            {
                @Override
                protected void compute(TimeRange result,
                                       ZonedDateTime now,
                                       boolean alignToBoundary)
                {
                    now = TimeUtils.truncateToQuarter(now);

                    result.start = now;
                    result.end   = now.plusMonths(3);
                }
            },
    PreviousQuarter
            {
                @Override
                protected void compute(TimeRange result,
                                       ZonedDateTime now,
                                       boolean alignToBoundary)
                {
                    now = TimeUtils.truncateToQuarter(now);

                    result.start = now.minusMonths(3);
                    result.end   = now;
                }
            },
    Last3Months
            {
                @Override
                protected void compute(TimeRange result,
                                       ZonedDateTime now,
                                       boolean alignToBoundary)
                {
                    if (alignToBoundary)
                    {
                        now = now.truncatedTo(ChronoUnit.DAYS);
                    }

                    result.start = now.minusMonths(3);
                    result.end   = now;
                }
            },
    Year
            {
                @Override
                protected void compute(TimeRange result,
                                       ZonedDateTime now,
                                       boolean alignToBoundary)
                {
                    now = TimeUtils.truncateToYear(now);

                    result.start = now;
                    result.end   = now.plusYears(1);
                }
            },
    PreviousYear
            {
                @Override
                protected void compute(TimeRange result,
                                       ZonedDateTime now,
                                       boolean alignToBoundary)
                {
                    now = TimeUtils.truncateToYear(now);

                    result.start = now.minusYears(1);
                    result.end   = now;
                }
            },
    Last365Days
            {
                @Override
                protected void compute(TimeRange result,
                                       ZonedDateTime now,
                                       boolean alignToBoundary)
                {
                    if (alignToBoundary)
                    {
                        now = now.truncatedTo(ChronoUnit.DAYS);
                    }

                    result.start = now.minus(365, ChronoUnit.DAYS);
                    result.end   = now;
                }
            },
    All
            {
                @Override
                protected void compute(TimeRange result,
                                       ZonedDateTime now,
                                       boolean alignToBoundary)
                {
                    // No start or end
                }
            };

    protected abstract void compute(TimeRange result,
                                    ZonedDateTime now,
                                    boolean alignToBoundary);

    public TimeRange resolve(ZonedDateTime time,
                             boolean alignToBoundary)
    {
        TimeRange result = new TimeRange();

        compute(result, time, alignToBoundary);

        return result;
    }
}
