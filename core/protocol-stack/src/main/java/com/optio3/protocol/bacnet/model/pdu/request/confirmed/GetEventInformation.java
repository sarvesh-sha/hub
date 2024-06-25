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
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceRequest;
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceResponse;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.protocol.model.bacnet.constructed.choice.BACnetTimeStamp;
import com.optio3.protocol.model.bacnet.enums.BACnetEventState;
import com.optio3.protocol.model.bacnet.enums.BACnetNotifyType;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetEventTransitionBits;
import com.optio3.serialization.SerializationTag;

public final class GetEventInformation extends ConfirmedServiceRequest
{
    public static final class Ack extends ConfirmedServiceResponse<GetEventInformation>
    {
        public static final class Values extends Sequence
        {
            @SerializationTag(number = 0)
            public BACnetObjectIdentifier object_identifier;

            @SerializationTag(number = 1)
            public BACnetEventState event_state;

            @SerializationTag(number = 2)
            public BACnetEventTransitionBits acknowledged_transitions;

            @SerializationTag(number = 3)
            public List<BACnetTimeStamp> event_timestamps; // SEQUENCE SIZE (3) OF BACnetTimeStamp,

            @SerializationTag(number = 4)
            public BACnetNotifyType notify_type;

            @SerializationTag(number = 5)
            public BACnetEventTransitionBits event_enable;

            @SerializationTag(number = 6)
            public List<Unsigned32> event_priorities = Lists.newArrayList();
        }

        @SerializationTag(number = 0)
        public List<Values> list_of_event_summaries = Lists.newArrayList();

        @SerializationTag(number = 1)
        public boolean more_events;
    }

    //--//

    @SerializationTag(number = 0)
    public Optional<BACnetObjectIdentifier> last_received_object_identifier;
}
