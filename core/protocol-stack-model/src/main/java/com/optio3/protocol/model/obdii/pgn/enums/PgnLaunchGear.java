/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum PgnLaunchGear implements TypedBitSet.ValueGetter
{
    // @formatter:off
    UseDefault                      (0),
    LaunchIn1stGear                 (1),
    LaunchIn2ndGear                 (2),
    LaunchIn3rdGear                 (3),
    LaunchIn4thGear                 (4),
    LaunchIn5thGear                 (5),
    LaunchIn6thGear                 (6),
    LaunchIn7thGear                 (7),
    LaunchIn8thGear                 (8),
    LaunchInReverse1                (9),
    LaunchInReverse2                (10),
    LaunchInReverse3                (11),
    LaunchInReverse4                (12),
    AutoOptimumLaunchGear           (13),
    Error                           (14),
    NotAvailable                    (15);
    // @formatter:on

    private final byte m_encoding;

    PgnLaunchGear(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static PgnLaunchGear parse(byte value)
    {
        for (PgnLaunchGear t : values())
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
