/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed;

import java.util.Optional;

import com.optio3.lang.Unsigned8;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.protocol.model.bacnet.enums.BACnetLiftCarDirection;
import com.optio3.protocol.model.bacnet.serialization.BACnetSerializationTag;
import com.optio3.serialization.SerializationTag;

public final class BACnetLandingCallStatus extends Sequence
{
    @SerializationTag(number = 0)
    public Unsigned8 floor_number;

    @SerializationTag(number = 1)
    @BACnetSerializationTag(choiceSet = "command")
    public BACnetLiftCarDirection direction;

    @SerializationTag(number = 2)
    @BACnetSerializationTag(choiceSet = "command")
    public Unsigned8 destination;

    @SerializationTag(number = 3)
    public Optional<String> floor_text;
}
