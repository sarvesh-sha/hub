/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum PgnFanState implements TypedBitSet.ValueGetter
{
    // @formatter:off
    Off                                (0),
    General                            (1),
    ExcessiveEngineAirTemperature      (2),
    ExcessiveEngineOilTemperature      (3),
    ExcessiveEngineCoolantTemperature  (4),
    ExcessiveTransmissionOilTemperature(5),
    ExcessiveHydraulicOilTemperature   (6),
    DefaultOperation                   (7),
    ReverseOperation                   (8),
    ManualControl                      (9),
    TransmissionRetarder               (10),
    AirConditioning                    (11),
    Timer                              (12),
    EngineBrake                        (13),
    Other                              (14),
    NotAvailable                       (15);
    // @formatter:on

    private final byte m_encoding;

    PgnFanState(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static PgnFanState parse(byte value)
    {
        for (PgnFanState t : values())
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
