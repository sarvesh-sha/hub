/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetNetworkPortCommand implements TypedBitSet.ValueGetter
{
    // @formatter:off
    idle                   (0),
    discard_changes        (1),
    renew_fd_registration  (2),
    restart_slave_discovery(3),
    renew_dhcp             (4),
    restart_autonegotiation(5),
    disconnect             (6),
    restart_port           (7);
    // @formatter:on

    private final byte m_encoding;

    BACnetNetworkPortCommand(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetNetworkPortCommand parse(byte value)
    {
        for (BACnetNetworkPortCommand t : values())
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
