/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.text;

import java.util.List;

import com.google.common.collect.Lists;

public class MultiCharacterSplit
{
    public static class Segment
    {
        public String  value;
        final  boolean isSeparator;

        Segment(String value,
                boolean isSeparator)
        {
            this.value       = value;
            this.isSeparator = isSeparator;
        }
    }

    private final List<Segment> m_parts = Lists.newArrayList();

    public MultiCharacterSplit(String text,
                               String separators,
                               boolean preserveAllTokens)
    {
        StringBuilder sb          = new StringBuilder();
        boolean       inSeparator = false;

        for (int pos = 0; pos < text.length(); pos++)
        {
            char    c           = text.charAt(pos);
            boolean isSeparator = separators.indexOf(c) >= 0;
            boolean isNumber    = Character.isDigit(c);

            if (!preserveAllTokens && inSeparator && isSeparator && c == text.charAt(pos - 1) && !isNumber)
            {
                // remove duplicate separators
                continue;
            }

            if (isSeparator != inSeparator)
            {
                addPart(sb, inSeparator);

                inSeparator = isSeparator;
            }

            sb.append(c);
        }

        addPart(sb, inSeparator);
    }

    public String join()
    {
        StringBuilder sb = new StringBuilder();
        for (Segment seg : m_parts)
        {
            sb.append(seg.value);
        }
        return sb.toString();
    }

    public String join(String newSeparator)
    {
        StringBuilder sb = new StringBuilder();
        for (Segment seg : m_parts)
        {
            if (!seg.isSeparator)
            {
                if (sb.length() > 0)
                {
                    sb.append(newSeparator);
                }
                sb.append(seg.value);
            }
        }
        return sb.toString();
    }

    public List<Segment> getParts()
    {
        return extract(false);
    }

    public List<Segment> getSeparators()
    {
        return extract(true);
    }

    private List<Segment> extract(boolean isSeparator)
    {
        List<Segment> res = Lists.newArrayList();
        for (Segment segment : m_parts)
        {
            if (segment.isSeparator == isSeparator)
            {
                res.add(segment);
            }
        }
        return res;
    }

    //--//

    private void addPart(StringBuilder sb,
                         boolean inSeparator)
    {
        m_parts.add(new Segment(sb.toString(), inSeparator));

        sb.setLength(0);
    }
}
