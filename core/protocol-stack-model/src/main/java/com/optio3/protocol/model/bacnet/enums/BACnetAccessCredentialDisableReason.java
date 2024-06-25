/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetAccessCredentialDisableReason implements TypedBitSet.ValueGetter
{
    // @formatter:off
    disabled                   (0),
    disabled_needs_provisioning(1),
    disabled_unassigned        (2),
    disabled_not_yet_active    (3),
    disabled_expired           (4),
    disabled_lockout           (5),
    disabled_max_days          (6),
    disabled_max_uses          (7),
    disabled_inactivity        (8),
    disabled_manual            (9);
    // @formatter:on

    private final byte m_encoding;

    BACnetAccessCredentialDisableReason(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetAccessCredentialDisableReason parse(byte value)
    {
        for (BACnetAccessCredentialDisableReason t : values())
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
