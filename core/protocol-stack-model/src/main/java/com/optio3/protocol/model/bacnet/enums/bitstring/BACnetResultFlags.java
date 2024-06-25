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

public final class BACnetResultFlags extends TypedBitSet<BACnetResultFlags.Values>
{
    public enum Values implements TypedBitSet.ValueGetter
    {
        // @formatter:off
        first_item(0), 
        last_item (1), 
        more_items(2);
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

    public BACnetResultFlags()
    {
        super(Values.class, Values.values());
    }

    public BACnetResultFlags(BitSet bs)
    {
        super(Values.class, Values.values(), bs);
    }

    @JsonCreator
    public BACnetResultFlags(List<String> inputs)
    {
        super(Values.class, Values.values(), inputs);
    }
}
