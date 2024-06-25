/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.logging;

import java.util.List;

import com.google.common.collect.Lists;

public class LogToStringVisitor implements LogVisitor
{
    private final ColumnarLogAdapter m_adapter;
    private final List<String>       m_lines = Lists.newArrayList();

    public LogToStringVisitor()
    {
        m_adapter = new ColumnarLogAdapter(this);
    }

    public void clear()
    {
        m_adapter.clear();

        m_lines.clear();
    }

    public List<String> getResults()
    {
        m_adapter.flush();

        return m_lines;
    }

    @Override
    public void accept(String line)
    {
        m_lines.add(line);
    }

    //--//

    public int getCurrentLogColumn()
    {
        return m_adapter.getCurrentLogColumn();
    }

    public void logToColumn(int column)
    {
        m_adapter.logToColumn(column);
    }

    public void indentTo(int minimum,
                         int incrementQuantum)
    {
        m_adapter.indentTo(minimum, incrementQuantum);
    }

    public void log(String fmt,
                    Object... args)
    {
        m_adapter.log(fmt, args);
    }

    public void logln(String fmt,
                      Object... args)
    {
        m_adapter.logln(fmt, args);
    }

    public void logln()
    {
        m_adapter.logln();
    }
}
