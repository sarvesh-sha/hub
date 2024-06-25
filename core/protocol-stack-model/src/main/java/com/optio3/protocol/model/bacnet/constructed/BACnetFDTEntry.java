/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed;

import com.optio3.lang.Unsigned16;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.serialization.SerializationTag;

public final class BACnetFDTEntry extends Sequence
{
    @SerializationTag(number = 0)
    public byte[] bacnetip_address; // the 6-octet B/IP or 18-octet B/IPv6 address of the registrant

    @SerializationTag(number = 1)
    public Unsigned16 time_to_live;

    @SerializationTag(number = 2)
    public Unsigned16 remaining_time_to_live;
}
