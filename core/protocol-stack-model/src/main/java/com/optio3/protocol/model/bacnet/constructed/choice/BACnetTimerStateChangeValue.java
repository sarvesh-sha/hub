/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed.choice;

import com.optio3.protocol.model.bacnet.Choice;
import com.optio3.protocol.model.bacnet.constructed.BACnetDateTime;
import com.optio3.protocol.model.bacnet.constructed.BACnetLightingCommand;
import com.optio3.serialization.SerializationTag;

public final class BACnetTimerStateChangeValue extends Choice
{
    @SerializationTag(number = 0)
    public final Object no_value = null;

    @SerializationTag(number = 1)
    public Object constructed_value;

    @SerializationTag(number = 2)
    public BACnetDateTime datetime;

    @SerializationTag(number = 3)
    public BACnetLightingCommand lighting_command;
}
