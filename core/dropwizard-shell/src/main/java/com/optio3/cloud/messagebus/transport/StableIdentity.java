/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus.transport;

import java.net.InetSocketAddress;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.optio3.logging.ILogger;
import com.optio3.util.TimeUtils;

public class StableIdentity
{
    public static class Statistics
    {
        public long connections;
        public long messagesRx;
        public long messagesTx;
        public long bytesRx;
        public long bytesTx;

        private long m_hour;

        public void accumulate(Statistics stats)
        {
            connections += stats.connections;
            messagesRx += stats.messagesRx;
            messagesTx += stats.messagesTx;
            bytesRx += stats.bytesRx;
            bytesTx += stats.bytesTx;
        }

        public void clear()
        {
            connections = 0;
            messagesRx  = 0;
            messagesTx  = 0;
            bytesRx     = 0;
            bytesTx     = 0;
        }
    }

    private final static int c_hours = 36;

    public final Statistics        totalStatistics  = new Statistics();
    public final Statistics[]      hourlyStatistics = new Statistics[c_hours];
    public       long              lastTimestampUTC;
    public       String            displayName;
    public       String            rpcId;
    public       InetSocketAddress lastKnownConnection;
    public       ILogger           logger;

    //--//

    public StableIdentity()
    {
        updateTimestamp();

        for (int i = 0; i < hourlyStatistics.length; i++)
        {
            hourlyStatistics[i] = new Statistics();
        }
    }

    //--//

    public void updateTimestamp()
    {
        lastTimestampUTC = TimeUtils.nowMilliUtc();
    }

    public ZonedDateTime getTimestamp()
    {
        return TimeUtils.fromSecondsToUtcTime(lastTimestampUTC);
    }

    public ZonedDateTime getLocalTimestamp()
    {
        return getTimestamp().withZoneSameInstant(ZoneId.systemDefault());
    }

    public void accumulate(Statistics stats)
    {
        long nowHours  = TimeUtils.nowMilliUtc() / 3_600_000;
        int  hourOfDay = (int) (nowHours % c_hours);

        totalStatistics.accumulate(stats);

        Statistics hourly = hourlyStatistics[hourOfDay];
        if (hourly.m_hour != nowHours)
        {
            // Reset stats when we roll over to the next day.

            hourly.m_hour = nowHours;
            hourly.clear();
        }

        hourly.accumulate(stats);

        stats.clear();
    }

    public Statistics report(int first,
                             int count)
    {
        long nowHours = TimeUtils.nowMilliUtc() / 3_600_000;

        first = first % c_hours;
        count = Math.min(c_hours, count);

        Statistics res = new Statistics();

        for (int i = 0; i < count; i++)
        {
            int        hourOfDay = (int) ((nowHours - (first + i)) % c_hours);
            Statistics hourly    = hourlyStatistics[hourOfDay];

            res.accumulate(hourly);
        }

        return res;
    }
}
