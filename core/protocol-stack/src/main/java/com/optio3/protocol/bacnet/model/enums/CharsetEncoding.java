/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.enums;

import java.nio.charset.Charset;

public enum CharsetEncoding
{
    // @formatter:off
    UTF_8     (0x00, "UTF-8"),
    DBCS      (0x01, "DBCS"),
    UCS_2     (0x02, "UTF-16BE"),
    UCS_4     (0x03, "UTF-32"),
    UCS_2b    (0x04, "UTF-16"),
    ISO_8859_1(0x05, "ISO-8859-1");
    // @formatter:on

    private final byte    m_encoding;
    private final Charset m_charset;

    CharsetEncoding(int encoding,
                    String name)
    {
        Charset charset;

        try
        {
            charset = name != null ? Charset.forName(name) : null;
        }
        catch (Exception e)
        {
            // Not supported charset.
            charset = null;
        }

        m_encoding = (byte) encoding;
        m_charset  = charset;
    }

    public static CharsetEncoding parse(byte value)
    {
        for (CharsetEncoding t : values())
        {
            if (t.m_encoding == value)
            {
                return t;
            }
        }

        return null;
    }

    public byte encoding()
    {
        return m_encoding;
    }

    public Charset charset()
    {
        return m_charset;
    }
}
