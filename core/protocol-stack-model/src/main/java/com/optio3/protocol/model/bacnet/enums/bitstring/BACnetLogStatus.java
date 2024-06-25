/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums.bitstring;

import java.util.BitSet;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.optio3.serialization.TypedBitSet;

public final class BACnetLogStatus extends TypedBitSet<BACnetLogStatus.Values>
{
    public enum Values implements TypedBitSet.ValueGetter
    {
        // @formatter:off
        log_disabled (0),
        buffer_purged (1),
        log_interrupted (2);
        // @formatter:on

        private final int m_offset;

        Values(int offset)
        {
            this.m_offset = offset;
        }

        @Override
        public int getEncodingValue()
        {
            return m_offset;
        }
    }

    public BACnetLogStatus()
    {
        super(Values.class, Values.values());
    }

    public BACnetLogStatus(BitSet bs)
    {
        super(Values.class, Values.values(), bs);
    }

    @JsonCreator
    public BACnetLogStatus(List<String> inputs)
    {
        super(Values.class, Values.values(), inputs);
    }
}
