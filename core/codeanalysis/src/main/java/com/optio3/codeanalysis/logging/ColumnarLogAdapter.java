/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.logging;

public class ColumnarLogAdapter
{
    public boolean useUnderline;

    private final LogVisitor    m_visitor;
    private final StringBuilder m_sb = new StringBuilder();
    private       int           m_indent;

    public ColumnarLogAdapter(LogVisitor visitor)
    {
        m_visitor = visitor;
    }

    public void clear()
    {
        m_sb.setLength(0);
        m_indent = 0;
    }

    public void flush()
    {
        if (m_sb.length() > 0)
        {
            logln();
        }
    }

    public int getCurrentLogColumn()
    {
        return m_indent;
    }

    public void logToColumn(int column)
    {
        int     count      = 0;
        boolean justSpaces = true;

        while (m_indent < column)
        {
            if ((m_indent % 20) == 0)
            {
                justSpaces = false;
            }

            if (justSpaces || count < 4)
            {
                log(" ");
            }
            else if (useUnderline)
            {
                log((m_indent % 2) == 0 ? "_" : " ");
            }
            else
            {
                log(" ");
            }

            count++;
        }
    }

    public void indentTo(int minimum,
                         int incrementQuantum)
    {
        int column = getCurrentLogColumn();
        if (column < minimum)
        {
            column = minimum;
        }
        else
        {
            column = (column + incrementQuantum);
            column -= column % incrementQuantum;
        }
        logToColumn(column);
    }

    //--//

    public void logMultiLine(String txt)
    {
        int column = getCurrentLogColumn();

        String[] parts = txt.split("\n");
        for (String part : parts)
        {
            if (part.length() == 0)
            {
                continue;
            }

            logToColumn(column);
            logln("%s", part);
        }
    }

    public void log(String fmt,
                    Object... args)
    {
        String str = String.format(fmt, args);
        m_sb.append(str);
        m_indent += str.length();
    }

    public void logln(String fmt,
                      Object... args)
    {
        log(fmt, args);
        logln();
    }

    public void logln()
    {
        String line = m_sb.toString();
        m_visitor.accept(line);

        m_sb.setLength(0);
        m_indent = 0;
    }
}
