/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.request.unconfirmed;

import java.util.Optional;

import com.optio3.lang.Unsigned32;
import com.optio3.protocol.bacnet.model.pdu.request.UnconfirmedServiceRequest;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.Choice;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.protocol.model.bacnet.serialization.BACnetSerializationTag;
import com.optio3.serialization.SerializationTag;

public class WhoHas extends UnconfirmedServiceRequest
{
    public static class Limits extends Sequence
    {
        @SerializationTag(number = 0)
        public Optional<Unsigned32> device_instance_range_low_limit;

        @SerializationTag(number = 1)
        public Optional<Unsigned32> device_instance_range_high_limit;
    }

    public static class Target extends Choice
    {
        @SerializationTag(number = 2)
        public BACnetObjectIdentifier object_identifier;

        @SerializationTag(number = 3)
        public String object_name;
    }

    @SerializationTag(number = 0)
    @BACnetSerializationTag(untagged = true)
    public Limits limits;

    @SerializationTag(number = 1)
    @BACnetSerializationTag(untagged = true)
    public Target object;
}
