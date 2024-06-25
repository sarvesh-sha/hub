/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.logging.Severity;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

public class LogBlock
{
    public List<LogEntry> items = Lists.newArrayList();

    //--//

    public void addLine(String line)
    {
        LogEntry entry = new LogEntry();
        entry.fd        = 1;
        entry.timestamp = TimeUtils.now();
        entry.line      = line;

        addLine(entry);
    }

    public void addLine(LogEntry entry)
    {
        items.add(entry);
    }

    //--//

    void fixup()
    {
        for (LogEntry item : items)
        {
            if (item.selector == null) // Fixup for legacy log format
            {
                try
                {
                    int pos1 = StringUtils.indexOf(item.line, " : ");
                    if (pos1 < 0)
                    {
                        continue;
                    }

                    int pos2 = StringUtils.indexOf(item.line, " : ", pos1 + 3);
                    if (pos2 < 0)
                    {
                        continue;
                    }

                    int pos3 = StringUtils.indexOf(item.line, " : ", pos2 + 3);
                    if (pos3 < 0)
                    {
                        continue;
                    }

                    item.level    = Severity.valueOf(extractPart(item.line, 0, pos1, true));
                    item.thread   = extractPart(item.line, pos1 + 3, pos2, true);
                    item.selector = extractPart(item.line, pos2 + 3, pos3, true);
                    item.line     = extractPart(item.line, pos3 + 3, item.line.length(), false);
                }
                catch (Throwable t)
                {
                    // Ignore upgrade failures.
                }
            }
        }
    }

    private String extractPart(String line,
                               int start,
                               int end,
                               boolean trim)
    {
        line = line.substring(start, end);

        if (trim)
        {
            line = line.trim();
        }

        return line;
    }
}
