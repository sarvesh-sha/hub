/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetEventType implements TypedBitSet.ValueGetter
{
    // @formatter:off
    change_of_bitstring      (0),
    change_of_state          (1),
    change_of_value          (2),
    command_failure          (3),
    floating_limit           (4),
    out_of_range             (5),
    complex_event_type       (6),
    deprecated1              (7),
    change_of_life_safety    (8),
    extended                 (9),
    buffer_ready             (10),
    unsigned_range           (11),
    reserved1                (12),
    access_event             (13),
    double_out_of_range      (14),
    signed_out_of_range      (15),
    unsigned_out_of_range    (16),
    change_of_characterstring(17),
    change_of_status_flags   (18),
    change_of_reliability    (19),
    none                     (20),
    change_of_discrete_value (21),
    change_of_timer          (22);
    // @formatter:on

    private final byte m_encoding;

    BACnetEventType(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetEventType parse(byte value)
    {
        for (BACnetEventType t : values())
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
