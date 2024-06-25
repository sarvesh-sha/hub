/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetLifeSafetyMode implements TypedBitSet.ValueGetter
{
    // @formatter:off
    off                       (0),
    on                        (1),
    test                      (2),
    manned                    (3),
    unmanned                  (4),
    armed                     (5),
    disarmed                  (6),
    prearmed                  (7),
    slow                      (8),
    fast                      (9),
    disconnected              (10),
    enabled                   (11),
    disabled                  (12),
    automatic_release_disabled(13),
    default_mode              (14);
    // @formatter:on

    private final byte m_encoding;

    BACnetLifeSafetyMode(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetLifeSafetyMode parse(byte value)
    {
        for (BACnetLifeSafetyMode t : values())
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

    @Override
    public int getEncodingValue()
    {
        return m_encoding;
    }
}
