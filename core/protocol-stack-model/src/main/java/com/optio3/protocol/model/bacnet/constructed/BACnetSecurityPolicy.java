/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed;

import java.util.List;

import com.optio3.lang.Unsigned8;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.serialization.SerializationTag;

public final class BACnetSecurityPolicy extends Sequence
{
    @SerializationTag(number = 0)
    public Unsigned8 key_revision;

    @SerializationTag(number = 1)
    public BACnetDateTime activation_time; // UTC time, all wild if unknown

    @SerializationTag(number = 2)
    public BACnetDateTime expiration_time; // UTC time, all wild if infinite

    @SerializationTag(number = 3)
    public List<BACnetKeyIdentifier> key_ids;
}
