/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetNodeType implements TypedBitSet.ValueGetter
{
    // @formatter:off
    unknown       (0),
    system        (1),
    network       (2),
    device        (3),
    organizational(4),
    area          (5),
    equipment     (6),
    point         (7),
    collection    (8),
    property      (9),
    functional    (10),
    other         (11),
    subsystem     (12),
    building      (13),
    floor         (14),
    section       (15),
    module        (16),
    tree          (17),
    member        (18),
    protocol      (19),
    room          (20),
    zone          (21);
    // @formatter:on

    private final byte m_encoding;

    BACnetNodeType(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetNodeType parse(byte value)
    {
        for (BACnetNodeType t : values())
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
