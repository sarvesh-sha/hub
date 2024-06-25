/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import java.time.ZonedDateTime;
import java.util.Objects;

import com.optio3.logging.Severity;
import com.optio3.serialization.Reflection;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

public class LogEntry
{
    public int fd;

    public String        host;
    public String        thread;
    public String        selector;
    public Severity      level;
    public ZonedDateTime timestamp;
    public String        line;

    @Override
    public boolean equals(Object o)
    {
        LogEntry that = Reflection.as(o, LogEntry.class);
        if (that == null)
        {
            return false;
        }

        return equals(that);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(timestamp, line);
    }

    public boolean equals(LogEntry that)
    {
        return this == that || (fd == that.fd && TimeUtils.compare(timestamp, that.timestamp) == 0 && StringUtils.equals(line, that.line));
    }
}
