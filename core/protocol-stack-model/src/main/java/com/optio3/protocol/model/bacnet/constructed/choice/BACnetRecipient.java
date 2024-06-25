/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed.choice;

import com.optio3.protocol.model.bacnet.BACnetAddress;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.Choice;
import com.optio3.serialization.SerializationTag;

public final class BACnetRecipient extends Choice
{
    @SerializationTag(number = 0)
    public BACnetObjectIdentifier device;

    @SerializationTag(number = 1)
    public BACnetAddress address;
}
