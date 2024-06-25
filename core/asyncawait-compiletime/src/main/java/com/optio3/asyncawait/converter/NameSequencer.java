/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.asyncawait.converter;

class NameSequencer
{
    private final StringBuilder sb = new StringBuilder();
    private final String        m_prefix;
    private final String        m_suffix;
    private       int           m_sequence;

    NameSequencer(String prefix,
                  String suffix)
    {
        m_prefix = prefix;
        m_suffix = suffix;
        m_sequence = suffix == null ? 0 : -1;
    }

    String nextValue()
    {
        sb.setLength(0);
        sb.append(m_prefix);

        if (m_suffix != null)
        {
            sb.append(m_suffix);
        }

        if (m_sequence++ >= 0)
        {
            sb.append(m_sequence);
        }

        return sb.toString();
    }
}
