/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.common;

import com.optio3.cloud.persistence.LogEntry;

public class LogLine extends LogEntry
{
    public int lineNumber;

    public void copyFrom(LogEntry entry)
    {
        fd = entry.fd;

        host      = entry.host;
        thread    = entry.thread;
        selector  = entry.selector;
        level     = entry.level;
        timestamp = entry.timestamp;
        line      = entry.line;
    }
}
