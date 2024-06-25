/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetRejectReason implements TypedBitSet.ValueGetter
{
    // @formatter:off
    other                      (0),
    buffer_overflow            (1),
    inconsistent_parameters    (2),
    invalid_parameter_data_type(3),
    invalid_tag                (4),
    missing_required_parameter (5),
    parameter_out_of_range     (6),
    too_many_arguments         (7),
    undefined_enumeration      (8),
    unrecognized_service       (9);
    // @formatter:on

    private final byte m_encoding;

    BACnetRejectReason(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetRejectReason parse(byte value)
    {
        for (BACnetRejectReason t : values())
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
