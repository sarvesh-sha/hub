/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed;

import java.util.Optional;

import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.serialization.SerializationTag;

public final class BACnetBDTEntry extends Sequence
{
    @SerializationTag(number = 0)
    public BACnetHostNPort bbmd_address;

    @SerializationTag(number = 1)
    public Optional<byte[]> broadcast_mask;
}
