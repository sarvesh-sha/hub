/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.request.confirmed;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceRequest;
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceResponse;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.protocol.model.bacnet.enums.BACnetEventState;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetEventTransitionBits;
import com.optio3.protocol.model.bacnet.serialization.BACnetSerializationTag;
import com.optio3.serialization.SerializationTag;

public final class GetAlarmSummary extends ConfirmedServiceRequest
{
    public static final class Ack extends ConfirmedServiceResponse<GetAlarmSummary>
    {
        public static final class Values extends Sequence
        {
            @SerializationTag(number = 0)
            public BACnetObjectIdentifier object_identifier;

            @SerializationTag(number = 1)
            public BACnetEventState alarm_state;

            @SerializationTag(number = 2)
            public BACnetEventTransitionBits acknowledged_transitions;
        }

        // The payload is actually just a list of untagged Values.
        @SerializationTag(number = 0)
        @BACnetSerializationTag(untagged = true)
        public List<Values> values = Lists.newArrayList();
    }
}
