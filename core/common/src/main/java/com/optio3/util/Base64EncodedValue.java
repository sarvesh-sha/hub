/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.util;

import java.util.Base64;

public class Base64EncodedValue
{
    private final String m_encodedContents;

    public Base64EncodedValue(String encodedContents)
    {
        m_encodedContents = encodedContents;
    }

    public Base64EncodedValue(byte[] contents)
    {
        this(Base64.getEncoder()
                   .encodeToString(contents));
    }

    public byte[] getValue()
    {
        return Base64.getDecoder()
                     .decode(m_encodedContents);
    }

    @Override
    public String toString()
    {
        return m_encodedContents;
    }
}
