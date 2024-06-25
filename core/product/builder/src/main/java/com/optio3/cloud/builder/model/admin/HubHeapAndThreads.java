/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.admin;

import java.time.ZonedDateTime;
import java.util.List;

import com.google.common.collect.Lists;

public class HubHeapAndThreads
{
    public ZonedDateTime timestamp;
    public long          memoryMax;
    public long          memoryTotal;
    public long          memoryUsed;
    public long          memoryFree;
    public boolean       heapWarning;

    public final List<HubUniqueStackTrace> uniqueStackTraces = Lists.newArrayList();

    public boolean isFreeMemoryBelowThreshold(int percent,
                                              long minimumAvailable)
    {
        long available = memoryMax - memoryUsed;
        if (available > minimumAvailable)
        {
            return false;
        }

        return 100 * memoryUsed > percent * memoryMax;
    }

    public String dump()
    {
        StringBuilder sb = new StringBuilder();

        addLine(sb, "Memory Max  : %,d", memoryMax);
        addLine(sb, "Memory Total: %,d", memoryTotal);
        addLine(sb, "Memory Free : %,d", memoryFree);
        addLine(sb, "Memory Used : %,d", memoryUsed);

        for (HubUniqueStackTrace uniqueStackTrace : uniqueStackTraces)
        {
            addLine(sb, "");
            addLine(sb, "Found %d unique stack traces:", uniqueStackTrace.threads.size());
            for (String thread : uniqueStackTrace.threads)
            {
                addLine(sb, "  %s", thread);
            }

            for (String frame : uniqueStackTrace.frames)
            {
                addLine(sb, "    %s", frame);
            }
        }

        return sb.toString();
    }

    private void addLine(StringBuilder sb,
                         String fmt,
                         Object... args)
    {
        sb.append(String.format(fmt, args));
        sb.append("\n");
    }
}
