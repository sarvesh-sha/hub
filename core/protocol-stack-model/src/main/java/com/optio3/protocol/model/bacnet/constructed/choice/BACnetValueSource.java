/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed.choice;

import com.optio3.protocol.model.bacnet.BACnetAddress;
import com.optio3.protocol.model.bacnet.Choice;
import com.optio3.protocol.model.bacnet.constructed.BACnetDeviceObjectReference;
import com.optio3.serialization.SerializationTag;

public final class BACnetValueSource extends Choice
{
    @SerializationTag(number = 0)
    public Object none; // NULL

    @SerializationTag(number = 1)
    public BACnetDeviceObjectReference object;

    @SerializationTag(number = 2)
    public BACnetAddress address;
}
