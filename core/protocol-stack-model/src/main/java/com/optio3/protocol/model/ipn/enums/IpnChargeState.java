/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum IpnChargeState implements TypedBitSet.ValueGetter
{
    // @formatter:off
    ChargeOff                      (0),
    Acceptance                     (1),
    Float                          (2),
    Equalize                       (3),
    CurrentLimit                   (4),
    BulkNormalMPPT                 (8),
    BulkExperimentingMPPT          (9),
    BulkNormalMPPTInEqualize       (10),
    BulkExperimentingMPPTInEqualize(11);
    // @formatter:on

    private final byte m_encoding;

    IpnChargeState(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static IpnChargeState parse(byte value)
    {
        for (IpnChargeState t : values())
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
