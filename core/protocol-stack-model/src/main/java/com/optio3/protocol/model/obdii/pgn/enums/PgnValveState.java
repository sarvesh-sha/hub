/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum PgnValveState implements TypedBitSet.ValueGetter
{
    // @formatter:off
    Normal                    (0),
    Alarm                     (1),
    AlarmHighSeverity         (2),
    DerateActive              (3),
    ControlledShutdownActive  (4),
    UncontrolledShutdownActive(4),
    CalibrationOrTest         (5),
    Reserved6                 (6),
    Reserved7                 (7),
    Reserved8                 (8),
    Reserved9                 (9),
    Reserved10                (10),
    Reserved11                (11),
    Reserved12                (12),
    Reserved13                (13),
    Error                     (14),
    NotAvailable              (15);
    // @formatter:on

    private final byte m_encoding;

    PgnValveState(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static PgnValveState parse(byte value)
    {
        for (PgnValveState t : values())
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
