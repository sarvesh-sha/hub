/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.text;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

/**
 * Helper class to parse ANSI escape sequences out of the standard output of a process.
 */
public class AnsiParser
{
    public static class EscapeSequence
    {
        public List<Integer> parameters = Collections.emptyList();
        public char          code;
    }

    private String m_text;
    private int    m_cursor;

    //--//

    public static String removeBackspaces(String line)
    {
        final char backspace      = 8;
        final char carriageReturn = 13;

        if (line == null)
        {
            return null;
        }

        if (line.indexOf(backspace) < 0 && line.indexOf(carriageReturn) < 0)
        {
            return line;
        }

        StringBuilder sb  = new StringBuilder();
        int           pos = 0;

        for (char c : line.toCharArray())
        {
            if (c == backspace)
            {
                if (pos > 0)
                {
                    pos--;
                }
            }
            else if (c == carriageReturn)
            {
                pos = 0;
            }
            else
            {
                if (pos < sb.length())
                {
                    sb.setCharAt(pos, c);
                }
                else
                {
                    sb.append(c);
                }

                pos++;
            }
        }

//        System.out.printf("BEFORE: %s%n", line);
//        System.out.printf("AFTER : %s%n", sb.toString());

        return sb.toString();
    }

    public static String removeEscapeSequences(String line)
    {
        AnsiParser    parser = new AnsiParser();
        StringBuilder sb     = new StringBuilder();
        for (Object obj : parser.parse(line))
        {
            if (obj instanceof String)
            {
                sb.append((String) obj);
            }
        }

        return sb.toString();
    }

    //--//

    public List<Object> parse(String text)
    {
        m_text = text;
        m_cursor = 0;

        List<Object> res       = Lists.newArrayList();
        int          lastFlush = 0;

        while (true)
        {
            char c = charAtSafe(m_cursor);
            if (c == 0)
            {
                break;
            }

            if (c == 0x1b && charAtSafe(m_cursor + 1) == '[')
            {
                int pos = m_cursor;

                EscapeSequence es = removeEscapeSequence();
                if (es != null)
                {
                    res.add(m_text.substring(lastFlush, pos));
                    res.add(es);

                    lastFlush = m_cursor;
                }
                else
                {
                    m_cursor = pos + 1;
                }
            }
            else
            {
                m_cursor++;
            }
        }

        if (lastFlush < m_cursor)
        {
            res.add(m_text.substring(lastFlush, m_cursor));
        }

        return res;
    }

    private EscapeSequence removeEscapeSequence()
    {
        EscapeSequence es = new EscapeSequence();

        int savedCursor = m_cursor;

        // Skip prologue.
        m_cursor += 2;

        Integer value = parseNumber();
        while (value != null)
        {
            if (es.parameters.isEmpty())
            {
                es.parameters = Lists.newArrayList();
            }

            es.parameters.add(value);

            char c = charAtSafe(m_cursor);
            if (c == 0)
            {
                break;
            }

            if (c != ';')
            {
                break;
            }

            m_cursor++;

            value = parseNumber();
            if (value == null)
            {
                // Semicolon must be followed by another digit => backtrack and return no result.
                m_cursor = savedCursor;
                return null;
            }
        }

        es.code = charAtSafe(m_cursor++);

        if (es.code == 0)
        {
            // No character after prologue => backtrack and return no result.
            m_cursor = savedCursor;
            return null;
        }

        return es;
    }

    private Integer parseNumber()
    {
        Integer res = null;

        while (true)
        {
            char c = charAtSafe(m_cursor);
            if (!Character.isDigit(c))
            {
                return res;
            }

            m_cursor++;
            if (res == null)
            {
                res = 0;
            }

            res = res * 10 + (c - '0');
        }
    }

    private char charAtSafe(int pos)
    {
        return pos < m_text.length() ? m_text.charAt(pos) : 0;
    }
}
