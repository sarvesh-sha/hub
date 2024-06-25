/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.util;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.serialization.Reflection;
import org.apache.commons.lang3.StringUtils;

public class StackTraceAnalyzer implements Comparable<StackTraceAnalyzer>
{
    public final StackTraceElement[] elements;

    private StackTraceAnalyzer(StackTraceElement[] elements)
    {
        this.elements = elements;
    }

    @Override
    public boolean equals(Object o)
    {
        StackTraceAnalyzer that = Reflection.as(o, StackTraceAnalyzer.class);
        if (that == null)
        {
            return false;
        }

        return Arrays.equals(elements, that.elements);
    }

    @Override
    public int hashCode()
    {
        return Arrays.hashCode(elements);
    }

    @Override
    public int compareTo(StackTraceAnalyzer other)
    {
        int depth = 0;

        while (true)
        {
            StackTraceElement elThis  = depth < this.elements.length ? this.elements[depth] : null;
            StackTraceElement elOther = depth < other.elements.length ? other.elements[depth] : null;

            if (elThis == null)
            {
                return elOther == null ? 0 : -1;
            }
            else if (elOther == null)
            {
                return 1;
            }

            int diff = StringUtils.compareIgnoreCase(elThis.toString(), elOther.toString());
            if (diff != 0)
            {
                return diff;
            }

            depth++;
        }
    }

    //--//

    public static int numberOfThreads()
    {
        return ManagementFactory.getThreadMXBean()
                                .getAllThreadIds().length;
    }

    public static <T> Map<StackTraceAnalyzer, List<T>> summarize(Collection<T> sources,
                                                                 Function<T, StackTraceElement[]> callback)
    {
        Map<StackTraceAnalyzer, List<T>> uniqueStackTraces = Maps.newHashMap();

        for (T source : sources)
        {
            StackTraceAnalyzer st = new StackTraceAnalyzer(callback.apply(source));

            List<T> sourcesWithSameStackTrace = uniqueStackTraces.computeIfAbsent(st, (key) -> Lists.newArrayList());
            sourcesWithSameStackTrace.add(source);
        }

        return uniqueStackTraces;
    }

    public static Map<StackTraceAnalyzer, List<Thread>> allThreads()
    {
        Map<Thread, StackTraceElement[]> map = Thread.getAllStackTraces();

        return summarize(map.keySet(), (thread) -> map.get(thread));
    }

    public static Map<StackTraceAnalyzer, List<ThreadInfo>> allThreadInfos()
    {
        ThreadInfo[] threads = ManagementFactory.getThreadMXBean()
                                                .dumpAllThreads(true, true);

        Map<StackTraceAnalyzer, List<ThreadInfo>> results = summarize(Lists.newArrayList(threads), (thread) -> thread.getStackTrace());

        for (StackTraceAnalyzer st : results.keySet())
        {
            List<ThreadInfo> sameStackTrace = results.get(st);

            sameStackTrace.sort(StackTraceAnalyzer::compareThreadInfo);
        }

        return results;
    }

    private static int compareThreadInfo(ThreadInfo a,
                                         ThreadInfo b)
    {
        int diff = StringUtils.compareIgnoreCase(a.getThreadName(), b.getThreadName());
        if (diff == 0)
        {
            diff = Long.compare(a.getThreadId(), b.getThreadId());
        }

        return diff;
    }

    public static List<String> printThreadInfos(boolean includeMemInfo,
                                                Map<StackTraceAnalyzer, List<ThreadInfo>> map)
    {
        List<String> lines = Lists.newArrayList();

        printThreadInfos(includeMemInfo, lines, map);

        return lines;
    }

    public static void printThreadInfos(boolean includeMemInfo,
                                        List<String> lines,
                                        Map<StackTraceAnalyzer, List<ThreadInfo>> map)
    {
        if (includeMemInfo)
        {
            Runtime rt = Runtime.getRuntime();
            rt.gc();
            long total = rt.totalMemory();
            long free  = rt.freeMemory();
            long max   = rt.maxMemory();

            addLine(lines, "Memory, Max  : %,12d", max);
            addLine(lines, "Memory, Total: %,12d", total);
            addLine(lines, "Memory, Free : %,12d", free);
            addLine(lines, "Memory, Used : %,12d", total - free);
            addLine(lines, "------------------------------");
        }

        List<StackTraceAnalyzer> sortedStacks = Lists.newArrayList(map.keySet());
        sortedStacks.sort(StackTraceAnalyzer::compareTo);

        int totalThreads = 0;

        Thread currentThread = Thread.currentThread();

        StackTraceAnalyzer currentStack = null;

        List<StackTraceAnalyzer> runnable     = Lists.newArrayList();
        List<StackTraceAnalyzer> blocked      = Lists.newArrayList();
        List<StackTraceAnalyzer> waiting      = Lists.newArrayList();
        List<StackTraceAnalyzer> waitingTimed = Lists.newArrayList();
        List<StackTraceAnalyzer> other        = Lists.newArrayList();

        for (StackTraceAnalyzer st : sortedStacks)
        {
            if (st.elements.length > 0)
            {
                List<ThreadInfo> threadInfos = map.get(st);

                totalThreads += threadInfos.size();

                for (ThreadInfo threadInfo : threadInfos)
                {
                    if (threadInfo.getThreadId() == currentThread.getId())
                    {
                        currentStack = st;
                        break;
                    }
                }

                if (atLeastOne(threadInfos, Thread.State.RUNNABLE))
                {
                    runnable.add(st);
                }
                else if (atLeastOne(threadInfos, Thread.State.BLOCKED))
                {
                    blocked.add(st);
                }
                else if (atLeastOne(threadInfos, Thread.State.TIMED_WAITING))
                {
                    waitingTimed.add(st);
                }
                else if (atLeastOne(threadInfos, Thread.State.WAITING))
                {
                    waiting.add(st);
                }
                else
                {
                    other.add(st);
                }
            }
        }

        addLine(lines, "Analyzed %d threads:", totalThreads);

        dumpStackTraces(currentStack, runnable, map, lines);
        dumpStackTraces(currentStack, blocked, map, lines);
        dumpStackTraces(currentStack, waitingTimed, map, lines);
        dumpStackTraces(currentStack, waiting, map, lines);
        dumpStackTraces(currentStack, other, map, lines);

        // Dump the stack for the requesting thread at the bottom.
        if (currentStack != null)
        {
            dumpStackTrace(lines, map, currentStack);
        }
    }

    private static void dumpStackTraces(StackTraceAnalyzer currentStack,
                                        List<StackTraceAnalyzer> lst,
                                        Map<StackTraceAnalyzer, List<ThreadInfo>> map,
                                        List<String> lines)
    {
        for (StackTraceAnalyzer st : lst)
        {
            if (currentStack == st)
            {
                continue;
            }

            dumpStackTrace(lines, map, st);
        }
    }

    private static boolean atLeastOne(List<ThreadInfo> threadInfos,
                                      Thread.State state)
    {
        for (ThreadInfo threadInfo : threadInfos)
        {
            if (threadInfo.getThreadState() == state)
            {
                return true;
            }
        }

        return false;
    }

    private static void dumpStackTrace(List<String> lines,
                                       Map<StackTraceAnalyzer, List<ThreadInfo>> map,
                                       StackTraceAnalyzer st)
    {
        if (st.elements.length > 0)
        {
            List<ThreadInfo> sameStackTrace = map.get(st);

            addLine(lines, "");
            addLine(lines, "Found %d unique stack traces:", sameStackTrace.size());

            for (ThreadInfo ti : sameStackTrace)
            {
                addLine(lines, "   %s", printThreadInfo(ti));
            }

            int depth = 0;

            for (StackTraceElement ste : st.elements)
            {
                addLine(lines, "      (%d) at %s", depth++, ste);
            }
        }
    }

    private static void addLine(List<String> res,
                                String fmt,
                                Object... args)
    {
        if (args == null || args.length == 0)
        {
            res.add(fmt);
        }
        else
        {
            res.add(String.format(fmt, args));
        }
    }

    public static String printThreadInfo(ThreadInfo ti)
    {
        StringBuilder sb = new StringBuilder();

        sb.append("'")
          .append(ti.getThreadName())
          .append("' Id=")
          .append(ti.getThreadId());

        ThreadMXBean mxBean = ManagementFactory.getThreadMXBean();
        if (mxBean.isThreadCpuTimeSupported())
        {
            double cpu = mxBean.getThreadCpuTime(ti.getThreadId()) / 1E9;

            if (cpu < 0.001)
            {
                sb.append(String.format(" CPU(%,d usecs)", (int) (cpu * 1E6)));
            }
            else if (cpu < 10)
            {
                sb.append(String.format(" CPU(%,d msecs)", (int) (cpu * 1E3)));
            }
            else
            {
                sb.append(String.format(" CPU(%,d secs)", (int) cpu));
            }
        }

        sb.append(" ")
          .append(ti.getThreadState());

        if (ti.getLockName() != null)
        {
            sb.append(" on ")
              .append(ti.getLockName());
        }

        if (ti.getLockOwnerName() != null)
        {
            sb.append(" owned by '")
              .append(ti.getLockOwnerName())
              .append("' Id=")
              .append(ti.getLockOwnerId());
        }

        if (ti.isSuspended())
        {
            sb.append(" (suspended)");
        }

        if (ti.isInNative())
        {
            sb.append(" (in native)");
        }

        final LockInfo lockInfo = ti.getLockInfo();
        if (lockInfo != null)
        {
            Thread.State ts = ti.getThreadState();
            switch (ts)
            {
                case BLOCKED:
                    sb.append(" (blocked on ");
                    sb.append(lockInfo);
                    sb.append(")");
                    break;

                case WAITING:
                case TIMED_WAITING:
                    sb.append(" (waiting on ");
                    sb.append(lockInfo);
                    sb.append(")");
                    break;
            }
        }

        for (MonitorInfo mi : ti.getLockedMonitors())
        {
            sb.append(" (locked monitor ")
              .append(mi)
              .append(" at stack ")
              .append(mi.getLockedStackDepth())
              .append(")");
        }

        for (LockInfo li : ti.getLockedSynchronizers())
        {
            sb.append(" (locked synchronizer ")
              .append(li)
              .append(")");
        }

        return sb.toString();
    }
}
