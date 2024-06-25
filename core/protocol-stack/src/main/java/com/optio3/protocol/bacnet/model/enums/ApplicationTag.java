/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;

public enum ApplicationTag
{
    // @formatter:off
    Null                  (0 ),
    Boolean               (1 ),
    UnsignedInteger       (2 ),
    SignedInteger         (3 ),
    Real                  (4 ),
    Double                (5 ),
    OctetString           (6 ),
    CharacterString       (7 ),
    BitString             (8 ),
    Enumerated            (9 ),
    Date                  (10),
    Time                  (11),
    BACnetObjectIdentifier(12);
    // @formatter:on

    private final byte m_encoding;

    ApplicationTag(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static ApplicationTag parse(byte value)
    {
        for (ApplicationTag t : values())
        {
            if (t.m_encoding == value)
            {
                return t;
            }
        }

        return null;
    }

    @HandlerForEncoding
    public byte encoding()
    {
        return m_encoding;
    }
}
