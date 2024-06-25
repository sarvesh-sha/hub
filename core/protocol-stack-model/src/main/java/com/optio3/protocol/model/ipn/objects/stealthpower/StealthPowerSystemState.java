/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.stealthpower;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum StealthPowerSystemState implements TypedBitSet.ValueGetter
{
    // @formatter:off
    IGNITION_OFF    (0),
    IGNITION_ON     (1),
    ENGINE_RUNNING  (2),
    ESTART          (3),
    SHORELINE       (4),
    SHORELINE_ACC   (5),
    DISCHARGING     (6),
    ACCESSORY_ON    (7),
    OV_PROTECT      (8),
    VSP_UV_PROTECT  (9),
    VOEM_UV_PROTECT (10),
    OEM_STATE       (11);
    // @formatter:on

    private final byte m_encoding;

    StealthPowerSystemState(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static StealthPowerSystemState parse(byte value)
    {
        for (StealthPowerSystemState t : values())
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
