/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.request.confirmed;

import java.util.List;
import java.util.Optional;

import com.optio3.lang.Unsigned32;
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceRequest;
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceResponse;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.protocol.model.bacnet.constructed.BACnetDateTime;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetResultFlags;
import com.optio3.protocol.model.bacnet.serialization.BACnetSerializationTag;
import com.optio3.serialization.SerializationTag;

public final class ReadRange extends ConfirmedServiceRequest
{
    public static final class Ack extends ConfirmedServiceResponse<ReadRange>
    {
        @SerializationTag(number = 0)
        public BACnetObjectIdentifier object_identifier;

        @SerializationTag(number = 1)
        public BACnetPropertyIdentifierOrUnknown property_identifier;

        @SerializationTag(number = 2)
        @BACnetSerializationTag(propertyIndex = true)
        public Optional<Unsigned32> property_array_index;

        @SerializationTag(number = 3)
        public BACnetResultFlags result_flags;

        @SerializationTag(number = 4)
        public Unsigned32 item_count;

        @SerializationTag(number = 5)
        public List<Object> item_data; // Depends on object_identifier & property_identifier

        @SerializationTag(number = 6)
        public Optional<Unsigned32> first_sequence_number; // used only if item_count > 0 and the request was either of type by_sequence_number or by_time
    }

    //--//

    public static final class ByPosition extends Sequence
    {
        @SerializationTag(number = 0)
        public Unsigned32 reference_index;

        @SerializationTag(number = 1)
        public short count;
    }

    public static final class BySequenceNumber extends Sequence
    {
        @SerializationTag(number = 0)
        public Unsigned32 reference_sequence_number;

        @SerializationTag(number = 1)
        public short count;
    }

    public static final class ByTime extends Sequence
    {
        @SerializationTag(number = 0)
        public BACnetDateTime reference_time;

        @SerializationTag(number = 1)
        public short count;
    }

    @SerializationTag(number = 0)
    public BACnetObjectIdentifier object_identifier;

    @SerializationTag(number = 1)
    public BACnetPropertyIdentifierOrUnknown property_identifier;

    @SerializationTag(number = 2)
    @BACnetSerializationTag(propertyIndex = true)
    public Optional<Unsigned32> property_array_index;

    @SerializationTag(number = 3)
    public Optional<ByPosition> by_position;

    @SerializationTag(number = 6)
    public Optional<BySequenceNumber> by_sequence_number;

    @SerializationTag(number = 7)
    public Optional<ByTime> by_time;
}
