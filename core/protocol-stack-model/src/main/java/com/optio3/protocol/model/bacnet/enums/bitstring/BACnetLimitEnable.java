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

public final class BACnetLimitEnable extends TypedBitSet<BACnetLimitEnable.Values>
{
    public enum Values implements TypedBitSet.ValueGetter
    {
        // @formatter:off
        low_limit_enable (0),
        high_limit_enable(1);
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

    public BACnetLimitEnable()
    {
        super(Values.class, Values.values());
    }

    public BACnetLimitEnable(BitSet bs)
    {
        super(Values.class, Values.values(), bs);
    }

    @JsonCreator
    public BACnetLimitEnable(List<String> inputs)
    {
        super(Values.class, Values.values(), inputs);
    }
}
