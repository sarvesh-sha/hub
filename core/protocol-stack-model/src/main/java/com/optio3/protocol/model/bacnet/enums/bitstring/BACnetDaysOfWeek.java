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

public final class BACnetDaysOfWeek extends TypedBitSet<BACnetDaysOfWeek.Values>
{
    public enum Values implements TypedBitSet.ValueGetter
    {
        // @formatter:off
        monday   (0), 
        tuesday  (1), 
        wednesday(2), 
        thursday (3), 
        friday   (4), 
        saturday (5), 
        sunday   (6);
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

    public BACnetDaysOfWeek()
    {
        super(Values.class, Values.values());
    }

    public BACnetDaysOfWeek(BitSet bs)
    {
        super(Values.class, Values.values(), bs);
    }

    @JsonCreator
    public BACnetDaysOfWeek(List<String> inputs)
    {
        super(Values.class, Values.values(), inputs);
    }
}
