/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.request.confirmed;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;
import com.optio3.lang.Unsigned32;
import com.optio3.lang.Unsigned8;
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceRequest;
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceResponse;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.protocol.model.bacnet.constructed.BACnetRecipientProcess;
import com.optio3.protocol.model.bacnet.enums.BACnetAcknowledgmentFilter;
import com.optio3.protocol.model.bacnet.enums.BACnetEventState;
import com.optio3.protocol.model.bacnet.enums.BACnetEventStateFilter;
import com.optio3.protocol.model.bacnet.enums.BACnetEventType;
import com.optio3.protocol.model.bacnet.serialization.BACnetSerializationTag;
import com.optio3.serialization.SerializationTag;

public final class GetEnrollmentSummary extends ConfirmedServiceRequest
{
    public static final class Ack extends ConfirmedServiceResponse<GetEnrollmentSummary>
    {
        public static final class Values extends Sequence
        {
            @SerializationTag(number = 0)
            public BACnetObjectIdentifier object_identifier;

            @SerializationTag(number = 1)
            public BACnetEventType event_type;

            @SerializationTag(number = 2)
            public BACnetEventState event_state;

            @SerializationTag(number = 3)
            public Unsigned8 priority;

            @SerializationTag(number = 4)
            public Optional<Unsigned32> notification_class;
        }

        // The payload is actually just a list of untagged Values.
        @SerializationTag(number = 0)
        @BACnetSerializationTag(untagged = true)
        public List<Values> values = Lists.newArrayList();
    }

    //--//

    public static final class Priority extends Sequence
    {
        @SerializationTag(number = 0)
        public Unsigned8 min_priority;

        @SerializationTag(number = 1)
        public Unsigned8 max_priority;
    }

    @SerializationTag(number = 0)
    public BACnetAcknowledgmentFilter acknowledgment_filter;

    @SerializationTag(number = 1)
    public Optional<BACnetRecipientProcess> enrollment_filter;

    @SerializationTag(number = 2)
    public Optional<BACnetEventStateFilter> event_state_filter;

    @SerializationTag(number = 3)
    public Optional<BACnetEventType> event_type_filter;

    @SerializationTag(number = 4)
    public Optional<Priority> priority_filter;

    @SerializationTag(number = 5)
    public Optional<Unsigned32> notification_class_filter;
}
