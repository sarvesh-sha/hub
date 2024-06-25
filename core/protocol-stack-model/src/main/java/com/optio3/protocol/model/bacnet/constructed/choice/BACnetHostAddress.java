/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed.choice;

import com.optio3.protocol.model.bacnet.Choice;
import com.optio3.serialization.SerializationTag;

public final class BACnetHostAddress extends Choice
{
    @SerializationTag(number = 0)
    public Object none; // NULL

    @SerializationTag(number = 1)
    public byte[] ip_address;

    @SerializationTag(number = 2)
    public String name;
}
