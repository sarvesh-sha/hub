/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetLifeSafetyState implements TypedBitSet.ValueGetter
{
    // @formatter:off
    quiet           (0),
    pre_alarm       (1),
    alarm           (2),
    fault           (3),
    fault_pre_alarm (4),
    fault_alarm     (5),
    not_ready       (6),
    active          (7),
    tamper          (8),
    test_alarm      (9),
    test_active     (10),
    test_fault      (11),
    test_fault_alarm(12),
    holdup          (13),
    duress          (14),
    tamper_alarm    (15),
    abnormal        (16),
    emergency_power (17),
    delayed         (18),
    blocked         (19),
    local_alarm     (20),
    general_alarm   (21),
    supervisory     (22),
    test_supervisory(23);
    // @formatter:on

    private final byte m_encoding;

    BACnetLifeSafetyState(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetLifeSafetyState parse(byte value)
    {
        for (BACnetLifeSafetyState t : values())
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
