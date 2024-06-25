/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetNetworkType implements TypedBitSet.ValueGetter
{
    // @formatter:off
    ethernet  (0),
    arcnet    (1),
    mstp      (2),
    ptp       (3),
    lontalk   (4),
    ipv4      (5),
    zigbee    (6),
    virtual   (7),
    non_bacnet(8), // removed in version 1 revision 18
    ipv6      (9),
    serial    (10);
    // @formatter:on

    private final byte m_encoding;

    BACnetNetworkType(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetNetworkType parse(byte value)
    {
        for (BACnetNetworkType t : values())
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
