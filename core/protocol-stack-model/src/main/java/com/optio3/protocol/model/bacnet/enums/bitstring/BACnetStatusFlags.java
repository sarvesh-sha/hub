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

public final class BACnetStatusFlags extends TypedBitSet<BACnetStatusFlags.Values>
{
    public enum Values implements TypedBitSet.ValueGetter
    {
        // @formatter:off
        in_alarm      (0),
        fault         (1),
        overridden    (2),
        out_of_service(3);
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

    public BACnetStatusFlags()
    {
        super(Values.class, Values.values());
    }

    public BACnetStatusFlags(BitSet bs)
    {
        super(Values.class, Values.values(), bs);
    }

    @JsonCreator
    public BACnetStatusFlags(List<String> inputs)
    {
        super(Values.class, Values.values(), inputs);
    }
}
