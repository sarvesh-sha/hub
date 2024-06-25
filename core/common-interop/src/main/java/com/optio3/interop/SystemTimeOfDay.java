/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.interop;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.util.Exceptions;
import com.optio3.util.TimeUtils;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Structure;

public final class SystemTimeOfDay
{
    public static class timeval extends Structure
    {
        public long tv_sec;
        public int  tv_usec;

        @Override
        protected List<String> getFieldOrder()
        {
            return Lists.newArrayList("tv_sec", "tv_usec");
        }
    }

    public static class timezone extends Structure
    {
        public int tz_minuteswest;
        public int tz_dsttime;

        @Override
        protected List<String> getFieldOrder()
        {
            return Lists.newArrayList("tz_minuteswest", "tz_dsttime");
        }
    }

    //--//

    static
    {
        Native.register(SystemTimeOfDay.class, NativeLibrary.getInstance("c"));
    }

    private static native int gettimeofday(timeval tv,
                                           timezone tz);

    private static native int settimeofday(timeval tv,
                                           timezone tz);

    //--//

    public static ZonedDateTime getTimeOfDay()
    {
        timeval tv  = new timeval();
        int     res = gettimeofday(tv, null);
        if (res < 0)
        {
            throw Exceptions.newRuntimeException("Failed to get time of day: %d", Native.getLastError());
        }

        Instant instant = Instant.ofEpochSecond(tv.tv_sec, tv.tv_usec * 1000);
        return TimeUtils.fromInstantToLocalTime(instant);
    }

    public static void setTimeOfDay(ZonedDateTime time)
    {
        timeval tv = new timeval();

        tv.tv_sec = time.toEpochSecond();
        tv.tv_usec = time.getNano() / 1000;

        timezone tz = new timezone();
        tz.tz_minuteswest = time.getOffset()
                                .getTotalSeconds();

        int res = settimeofday(tv, tz);
        if (res < 0)
        {
            throw Exceptions.newRuntimeException("Failed to set time of day: %d", Native.getLastError());
        }
    }
}