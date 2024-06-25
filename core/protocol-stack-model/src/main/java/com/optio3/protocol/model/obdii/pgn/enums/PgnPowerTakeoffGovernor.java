/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum PgnPowerTakeoffGovernor implements TypedBitSet.ValueGetter
{
    // @formatter:off
    Off                (0),
    Hold               (1),
    RemoteHold         (2),
    Standby            (3),
    RemoteStandby      (4),
    Set                (5),
    Decelerate         (6),
    Resume             (7),
    Accelerate         (8),
    AcceleratorOverride(9),
    SetSpeed1          (10),
    SetSpeed2          (11),
    SetSpeed3          (12),
    SetSpeed4          (13),
    SetSpeed5          (14),
    SetSpeed6          (15),
    SetSpeed7          (16),
    SetSpeed8          (17),
    SetSpeedMemory1    (18),
    SetSpeedMemory2    (19),
    SetSpeedMemory3    (20),
    Reserved21         (21),
    Reserved22         (22),
    Reserved23         (23),
    Reserved24         (24),
    Reserved25         (25),
    Reserved26         (26),
    Reserved27         (27),
    Reserved28         (28),
    Reserved29         (29),
    Reserved30         (30),
    NotAvailable       (31);
    // @formatter:on

    private final byte m_encoding;

    PgnPowerTakeoffGovernor(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static PgnPowerTakeoffGovernor parse(byte value)
    {
        for (PgnPowerTakeoffGovernor t : values())
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
