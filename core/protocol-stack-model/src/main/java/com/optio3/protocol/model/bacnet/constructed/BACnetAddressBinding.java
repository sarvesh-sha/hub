/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed;

import com.optio3.protocol.model.bacnet.BACnetAddress;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.protocol.model.bacnet.serialization.BACnetSerializationTag;
import com.optio3.serialization.SerializationTag;

public final class BACnetAddressBinding extends Sequence
{
    @SerializationTag(number = 0)
    @BACnetSerializationTag(untagged = true)
    public BACnetObjectIdentifier device_identifier;

    @SerializationTag(number = 1)
    @BACnetSerializationTag(untagged = true)
    public BACnetAddress device_address;
}
