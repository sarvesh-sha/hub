/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import java.util.List;

import com.optio3.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class LogEntryFilterRequest
{
    public Integer fromOffset;
    public Integer toOffset;
    public Integer limit;

    public List<String> levels;
    public List<String> threads;
    public List<String> hosts;
    public List<String> selectors;

    public String filter;

    public boolean matches(LogEntry entry)
    {
        if (!matches(entry.level, levels))
        {
            return false;
        }

        if (!matches(entry.thread, threads))
        {
            return false;
        }

        if (!matches(entry.host, hosts))
        {
            return false;
        }

        if (!matches(entry.selector, selectors))
        {
            return false;
        }

        if (StringUtils.isBlank(filter))
        {
            return true;
        }

        if (StringUtils.containsIgnoreCase(entry.line, filter))
        {
            return true;
        }

        if (StringUtils.containsIgnoreCase(entry.selector, filter))
        {
            return true;
        }

        if (StringUtils.containsIgnoreCase(entry.thread, filter))
        {
            return true;
        }

        return false;
    }

    private static boolean matches(Enum<?> element,
                                   List<String> filters)
    {
        return matches(element != null ? element.name() : null, filters);
    }

    private static boolean matches(String element,
                                   List<String> filters)
    {
        if (CollectionUtils.isEmpty(filters))
        {
            return true;
        }

        for (String filter : filters)
        {
            boolean negate        = false;
            String  filterMutable = filter;

            if (filterMutable.startsWith("!"))
            {
                negate        = true;
                filterMutable = filterMutable.substring(1);
            }

            if (negate == StringUtils.equalsIgnoreCase(element, filterMutable))
            {
                return false;
            }
        }

        return true;
    }
}
