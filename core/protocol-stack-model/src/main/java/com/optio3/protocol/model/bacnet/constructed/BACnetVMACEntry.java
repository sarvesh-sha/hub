/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed;

import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.serialization.SerializationTag;

public final class BACnetVMACEntry extends Sequence
{
    @SerializationTag(number = 0)
    public byte[] virtual_mac_address;

    @SerializationTag(number = 1)
    public byte[] native_mac_address;
}
