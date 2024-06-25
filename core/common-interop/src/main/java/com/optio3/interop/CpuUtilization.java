/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.interop;

import java.lang.management.ThreadInfo;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.optio3.logging.ILogger;
import com.optio3.util.MonotonousTime;
import com.optio3.util.StackTraceAnalyzer;
import com.optio3.util.TimeUtils;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Platform;
import com.sun.jna.Structure;

public final class CpuUtilization
{
    private static final int _SC_CLK_TCK__MACOS = 3;
    private static final int _SC_CLK_TCK__LINUX = 2;

    private static final double s_oneHundredOverClocksPerMillisecond;

    //--//

    public static class DeltaSample
    {
        public final double userPercent;
        public final double systemPercent;

        public final double intervalInSeconds;

        public DeltaSample(Sample start,
                           Sample end)
        {
            long interval = end.instant.toEpochMilli() - start.instant.toEpochMilli();
            if (interval > 0)
            {
                double ratio = s_oneHundredOverClocksPerMillisecond / interval;

                systemPercent = (end.systemTime - start.systemTime) * ratio;
                userPercent = (end.userTime - start.userTime) * ratio;
                intervalInSeconds = interval / 1000.0;
            }
            else
            {
                userPercent = 0;
                systemPercent = 0;
                intervalInSeconds = 0;
            }
        }
    }

    public static class HighLoadMonitor
    {
        private final ILogger m_logger;
        private final int     m_initialLoadThreshold;
        private final int     m_incrementLoadThreshold;
        private final int     m_loadBackToNormal;
        private final int     m_delay;

        private int            m_highLoadThreshold;
        private boolean        m_warnedAboutHighLoad;
        private MonotonousTime m_delayForHighLoadReport;
        private MonotonousTime m_delayForNormalLoadReport;

        public HighLoadMonitor(ILogger logger,
                               int initialLoadThreshold,
                               int incrementLoadThreshold,
                               int loadBackToNormal,
                               int delay)
        {
            m_logger = logger;
            m_initialLoadThreshold = initialLoadThreshold;
            m_incrementLoadThreshold = incrementLoadThreshold;
            m_loadBackToNormal = loadBackToNormal;
            m_delay = delay;

            m_highLoadThreshold = initialLoadThreshold;
        }

        public void process(int cpuUsageUser)
        {
            if (cpuUsageUser > m_highLoadThreshold)
            {
                if (m_delayForHighLoadReport == null)
                {
                    m_delayForHighLoadReport = TimeUtils.computeTimeoutExpiration(m_delay, TimeUnit.MINUTES);
                }

                if (TimeUtils.isTimeoutExpired(m_delayForHighLoadReport))
                {
                    m_delayForHighLoadReport = null;

                    try
                    {
                        Map<StackTraceAnalyzer, List<ThreadInfo>> uniqueStackTraces = StackTraceAnalyzer.allThreadInfos();
                        List<String>                              lines             = StackTraceAnalyzer.printThreadInfos(true, uniqueStackTraces);

                        m_logger.warn("Detected CPU load above %d%%: %d%%", m_highLoadThreshold, cpuUsageUser);
                        m_logger.warn("");
                        for (String line : lines)
                        {
                            m_logger.warn(line);
                        }
                    }
                    catch (Throwable t)
                    {
                        // Ignore failures.
                    }

                    m_highLoadThreshold = cpuUsageUser + m_incrementLoadThreshold;
                    m_warnedAboutHighLoad = true;
                }
            }
            else
            {
                m_delayForHighLoadReport = null;
            }

            if (cpuUsageUser < m_loadBackToNormal)
            {
                if (m_warnedAboutHighLoad)
                {
                    if (m_delayForNormalLoadReport == null)
                    {
                        m_delayForNormalLoadReport = TimeUtils.computeTimeoutExpiration(m_delay, TimeUnit.MINUTES);
                    }

                    if (TimeUtils.isTimeoutExpired(m_delayForNormalLoadReport))
                    {
                        m_logger.warn("CPU load back to normal!");

                        m_highLoadThreshold = m_initialLoadThreshold;
                        m_warnedAboutHighLoad = false;
                    }
                }
            }
            else
            {
                m_delayForNormalLoadReport = null;
            }
        }
    }

    public static class Sample
    {
        public final Instant instant;

        public final long systemTime;
        public final long userTime;

        private Sample(Instant instant,
                       long systemTime,
                       long userTime)
        {
            this.instant = instant;
            this.systemTime = systemTime;
            this.userTime = userTime;
        }
    }

    //--//

    public static abstract class tms extends Structure
    {
        @Override
        protected List<String> getFieldOrder()
        {
            return Lists.newArrayList("tms_utime", "tms_stime", "tms_cutime", "tms_cstime");
        }

        abstract long getSystemTime();

        abstract long getUserTime();
    }

    public static class tms32 extends tms
    {
        public int tms_utime;
        public int tms_stime;
        public int tms_cutime;
        public int tms_cstime;

        @Override
        long getSystemTime()
        {
            return tms_stime;
        }

        @Override
        long getUserTime()
        {
            return tms_utime;
        }
    }

    public static class tms64 extends tms
    {
        public long tms_utime;
        public long tms_stime;
        public long tms_cutime;
        public long tms_cstime;

        @Override
        long getSystemTime()
        {
            return tms_stime;
        }

        @Override
        long getUserTime()
        {
            return tms_utime;
        }
    }

    //--//

    static
    {
        Native.register(CpuUtilization.class, NativeLibrary.getInstance("c"));

        long ticks = sysconf(Platform.isMac() ? _SC_CLK_TCK__MACOS : _SC_CLK_TCK__LINUX);
        if (ticks > 0)
        {
            s_oneHundredOverClocksPerMillisecond = 100 * 1000.0 / ticks;
        }
        else
        {
            s_oneHundredOverClocksPerMillisecond = 0;
        }
    }

    private static native int times(tms details);

    private static native int sysconf(int name);

    //--//

    public static Sample takeSample()
    {
        tms sample = getProcessTimes();

        return new Sample(Instant.now(), sample.getSystemTime(), sample.getUserTime());
    }

    private static tms getProcessTimes()
    {
        if (Platform.is64Bit())
        {
            tms64 res = new tms64();
            times(res);
            return res;
        }
        else
        {
            tms32 res = new tms32();
            times(res);
            return res;
        }
    }
}
