/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.logging;

import java.time.ZonedDateTime;

import com.optio3.util.TimeUtils;

public class ConsoleAppender implements ILoggerAppender
{
    private static final ColumnFormat s_threadColumn   = new ColumnFormat(50);
    private static final ColumnFormat s_selectorColumn = new ColumnFormat(Integer.MAX_VALUE);
    private static final ColumnFormat s_severityColumn = new ColumnFormat(Integer.MAX_VALUE);

    @Override
    public boolean append(ILogger context,
                          ZonedDateTime timestamp,
                          Severity level,
                          String thread,
                          String selector,
                          String msg)
    {
        String sb = s_severityColumn.format(level) + " [" + timestamp.format(TimeUtils.DEFAULT_FORMATTER_MILLI) + "] " + s_threadColumn.format(thread) + " : " + s_selectorColumn.format(selector) + " : " + msg;

        System.out.println(sb);

        return false; // Allow other appenders to see entry.
    }
}
