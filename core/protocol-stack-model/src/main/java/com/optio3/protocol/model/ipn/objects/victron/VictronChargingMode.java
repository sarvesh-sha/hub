/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.victron;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum VictronChargingMode
{
    // @formatter:off
    Off       (0),
    LowPower  (1),
    Fault     (2),
    Bulk      (3),
    Absorption(4),
    Float     (5),
    Inverting (9);
    // @formatter:on

    private final int m_encoding;

    VictronChargingMode(int encoding)
    {
        m_encoding = encoding;
    }

    public static VictronChargingMode parse(int value)
    {
        for (VictronChargingMode t : values())
        {
            if (t.m_encoding == value)
            {
                return t;
            }
        }

        return null;
    }
}
