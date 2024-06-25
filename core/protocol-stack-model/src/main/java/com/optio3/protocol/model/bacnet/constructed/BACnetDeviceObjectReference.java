/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed;

import java.util.Optional;

import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.serialization.SerializationTag;

public final class BACnetDeviceObjectReference extends Sequence
{
    @SerializationTag(number = 0)
    public Optional<BACnetObjectIdentifier> device_identifier;

    @SerializationTag(number = 1)
    public BACnetObjectIdentifier object_identifier;
}
