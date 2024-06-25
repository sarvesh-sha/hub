/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetAbortReason implements TypedBitSet.ValueGetter
{
    // @formatter:off
    other                               (0),
    buffer_overflow                     (1),
    invalid_apdu_in_this_state          (2),
    preempted_by_higher_priority_task   (3),
    segmentation_not_supported          (4),
    security_error                      (5),
    insufficient_security               (6),
    window_size_out_of_range            (7),
    application_exceeded_reply_time     (8),
    out_of_resources                    (9),
    tsm_timeout                         (10),
    apdu_too_long                       (11);
    // @formatter:on

    private final byte m_encoding;

    BACnetAbortReason(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetAbortReason parse(byte value)
    {
        for (BACnetAbortReason t : values())
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
