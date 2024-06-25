/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis;

public class JsonBuilder implements AutoCloseable
{
    protected final StringBuilder m_sb;
    private final   String        m_openText;
    private final   String        m_closeText;
    private         boolean       m_needSeparator;

    public JsonBuilder()
    {
        this(new StringBuilder(), "{", "}");
    }

    private JsonBuilder(StringBuilder sb,
                        String openText,
                        String closeText)
    {
        m_sb = sb;
        m_openText = openText;
        m_closeText = closeText;

        sb.append(m_openText);
    }

    public JsonBuilder newArray(String name)
    {
        emitName(name);

        return new JsonBuilder(m_sb, "[", "]");
    }

    public JsonBuilder newObject(String name)
    {
        emitName(name);

        return new JsonBuilder(m_sb, "{", "}");
    }

    public void newField(String name,
                         Object value)
    {
        emitName(name);
        emitValue(value);
    }

    public void newRawValue(Object value)
    {
        newField(null, value);
    }

    private void emitName(String name)
    {
        recordNewEntry();

        if (name != null)
        {
            emitValue(name);
            m_sb.append(": ");
        }
    }

    private void emitValue(Object val)
    {
        if (val == null)
        {
            m_sb.append("null");
        }
        else
        {
            m_sb.append('"');

            String txt = String.valueOf(val);
            txt = txt.replace("\\", "\\\\");
            txt = txt.replace("\"", "\\\"");
            m_sb.append(txt);

            m_sb.append('"');
        }
    }

    private void recordNewEntry()
    {
        if (m_needSeparator)
        {
            m_sb.append(", ");
        }

        m_needSeparator = true;
    }

    @Override
    public void close()
    {
        m_sb.append(m_closeText);
    }

    @Override
    public String toString()
    {
        return m_sb.toString();
    }
}
