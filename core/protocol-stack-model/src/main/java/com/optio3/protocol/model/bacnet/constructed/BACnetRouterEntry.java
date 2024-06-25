/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed;

import java.util.Optional;

import com.optio3.lang.Unsigned16;
import com.optio3.lang.Unsigned8;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.protocol.model.bacnet.enums.BACnetRouterEntryStatus;
import com.optio3.serialization.SerializationTag;

public final class BACnetRouterEntry extends Sequence
{
    @SerializationTag(number = 0)
    public Unsigned16 network_number;

    @SerializationTag(number = 1)
    public byte[] mac_address;

    @SerializationTag(number = 2)
    public BACnetRouterEntryStatus status;

    @SerializationTag(number = 3)
    public Optional<Unsigned8> performance_index;
}
