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

public final class BACnetFaultType extends TypedBitSet<BACnetFaultType.Values>
{
    public enum Values implements TypedBitSet.ValueGetter
    {
        // @formatter:off
        none                 (0), 
        fault_characterstring(1), 
        fault_extended       (2), 
        fault_life_safety    (3), 
        fault_state          (4), 
        fault_status_flags   (5), 
        fault_out_of_range   (6), 
        fault_listed         (7);
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

    public BACnetFaultType()
    {
        super(Values.class, Values.values());
    }

    public BACnetFaultType(BitSet bs)
    {
        super(Values.class, Values.values(), bs);
    }

    @JsonCreator
    public BACnetFaultType(List<String> inputs)
    {
        super(Values.class, Values.values(), inputs);
    }
}
