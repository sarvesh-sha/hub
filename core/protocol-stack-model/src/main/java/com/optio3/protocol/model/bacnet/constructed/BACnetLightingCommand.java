/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed;

import java.util.Optional;

import com.optio3.lang.Unsigned32;
import com.optio3.lang.Unsigned8;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.protocol.model.bacnet.enums.BACnetLightingOperation;
import com.optio3.serialization.SerializationTag;

public final class BACnetLightingCommand extends Sequence
{
    @SerializationTag(number = 0)
    public BACnetLightingOperation operation;

    @SerializationTag(number = 1)
    public Optional<Float> target_level;

    @SerializationTag(number = 2)
    public Optional<Float> ramp_rate;

    @SerializationTag(number = 3)
    public Optional<Float> step_increment;

    @SerializationTag(number = 4)
    public Optional<Unsigned32> fade_time;

    @SerializationTag(number = 5)
    public Optional<Unsigned8> priority;
}
