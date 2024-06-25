/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed;

import com.optio3.lang.Unsigned16;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.protocol.model.bacnet.constructed.choice.BACnetHostAddress;
import com.optio3.serialization.SerializationTag;

public final class BACnetHostNPort extends Sequence
{
    @SerializationTag(number = 0)
    public BACnetHostAddress host;

    @SerializationTag(number = 1)
    public Unsigned16 port;
}
