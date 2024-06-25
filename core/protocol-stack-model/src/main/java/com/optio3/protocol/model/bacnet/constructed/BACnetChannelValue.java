/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed;

import com.optio3.protocol.model.bacnet.AnyValue;
import com.optio3.serialization.SerializationTag;

public final class BACnetChannelValue extends AnyValue
{
    @SerializationTag(number = 0)
    public BACnetLightingCommand lighting_command;
}
