/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed;

import java.util.Optional;

import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.serialization.SerializationTag;

public final class BACnetSetpointReference extends Sequence
{
    @SerializationTag(number = 0)
    public Optional<BACnetObjectPropertyReference> setpoint_reference;
}
