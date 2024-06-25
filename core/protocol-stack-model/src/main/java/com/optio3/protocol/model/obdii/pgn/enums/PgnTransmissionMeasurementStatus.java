/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum PgnTransmissionMeasurementStatus implements TypedBitSet.ValueGetter
{
    // @formatter:off
    Valid                                         (0),
    NotValid_SettlingTimerStillCountingDown       (1),
    NotValid_TransmissionInGear                   (2),
    NotValid_TransmissionFluidTemperatureTooLow   (3),
    NotValid_TransmissionFluidTemperatureTooHigh  (4),
    NotValid_VehicleMoving_OutputShaftSpeedTooHigh(5),
    NotValid_VehicleNotLevel                      (6),
    NotValid_EngineSpeedTooLow                    (7),
    NotValid_EngineSpeedTooHigh                   (8),
    NotValid_NoRequestForReading                  (9),
    NotDefined10                                  (10),
    NotDefined11                                  (11),
    NotDefined12                                  (12),
    NotValid_Other                                (13),
    Error                                         (14),
    NotAvailable                                  (15);
    // @formatter:on

    private final byte m_encoding;

    PgnTransmissionMeasurementStatus(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static PgnTransmissionMeasurementStatus parse(byte value)
    {
        for (PgnTransmissionMeasurementStatus t : values())
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
