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

public final class BACnetEventTransitionBits extends TypedBitSet<BACnetEventTransitionBits.Values>
{
    public enum Values implements TypedBitSet.ValueGetter
    {
        // @formatter:off
        to_offnormal(0),
        to_fault    (1),
        to_normal   (2);
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

    public BACnetEventTransitionBits()
    {
        super(Values.class, Values.values());
    }

    public BACnetEventTransitionBits(BitSet bs)
    {
        super(Values.class, Values.values(), bs);
    }

    @JsonCreator
    public BACnetEventTransitionBits(List<String> inputs)
    {
        super(Values.class, Values.values(), inputs);
    }
}
