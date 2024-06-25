/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetBackupState implements TypedBitSet.ValueGetter
{
    // @formatter:off
    idle                 (0),
    preparing_for_backup (1),
    preparing_for_restore(2),
    performing_a_backup  (3),
    performing_a_restore (4),
    backup_failure       (5),
    restore_failure      (6);
    // @formatter:on

    private final byte m_encoding;

    BACnetBackupState(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetBackupState parse(byte value)
    {
        for (BACnetBackupState t : values())
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
