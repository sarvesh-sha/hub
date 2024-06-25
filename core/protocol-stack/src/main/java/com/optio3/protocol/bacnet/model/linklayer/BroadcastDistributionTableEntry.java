/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.linklayer;

import com.optio3.lang.Unsigned16;
import com.optio3.lang.Unsigned32;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.serialization.SerializationTag;

public final class BroadcastDistributionTableEntry extends Sequence
{
    @SerializationTag(number = 0)
    public Unsigned32 device_address;

    @SerializationTag(number = 1)
    public Unsigned16 port;

    @SerializationTag(number = 2)
    public Unsigned32 mask;
}
