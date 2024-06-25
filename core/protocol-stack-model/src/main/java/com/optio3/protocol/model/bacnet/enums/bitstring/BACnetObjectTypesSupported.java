/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums.bitstring;

import java.util.BitSet;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;
import com.optio3.serialization.TypedBitSet;

public final class BACnetObjectTypesSupported extends TypedBitSet<BACnetObjectType>
{
    public BACnetObjectTypesSupported()
    {
        super(BACnetObjectType.class, BACnetObjectType.values());
    }

    public BACnetObjectTypesSupported(BitSet bs)
    {
        super(BACnetObjectType.class, BACnetObjectType.values(), bs);
    }

    @JsonCreator
    public BACnetObjectTypesSupported(List<String> inputs)
    {
        super(BACnetObjectType.class, BACnetObjectType.values(), inputs);
    }
}
