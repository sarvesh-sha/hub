/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed;

import java.util.Optional;

import com.optio3.lang.Unsigned32;
import com.optio3.lang.Unsigned8;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.Choice;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.protocol.model.bacnet.constructed.choice.BACnetNotificationParameters;
import com.optio3.protocol.model.bacnet.constructed.choice.BACnetTimeStamp;
import com.optio3.protocol.model.bacnet.enums.BACnetEventState;
import com.optio3.protocol.model.bacnet.enums.BACnetEventType;
import com.optio3.protocol.model.bacnet.enums.BACnetNotifyType;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetLogStatus;
import com.optio3.serialization.SerializationTag;

public final class BACnetEventLogRecord extends Sequence
{
    @SerializationTag(number = 0)
    public BACnetDateTime timestamp;

    @SerializationTag(number = 1)
    public typefor_log_datum log_datum;

    public static final class typefor_log_datum extends Choice
    {
        @SerializationTag(number = 0)
        public BACnetLogStatus log_status;

        @SerializationTag(number = 1)
        public ConfirmedEventNotification_Request notification;

        @SerializationTag(number = 2)
        public float time_change;
    }

    public static final class ConfirmedEventNotification_Request
    {
        @SerializationTag(number = 0)
        public Unsigned32 process_identifier;

        @SerializationTag(number = 1)
        public BACnetObjectIdentifier initiating_device_identifier;

        @SerializationTag(number = 2)
        public BACnetObjectIdentifier event_object_identifier;

        @SerializationTag(number = 3)
        public BACnetTimeStamp timestamp;

        @SerializationTag(number = 4)
        public Unsigned32 notification_class;

        @SerializationTag(number = 5)
        public Unsigned8 priority;

        @SerializationTag(number = 6)
        public BACnetEventType event_type;

        @SerializationTag(number = 7)
        public Optional<String> message_text;

        @SerializationTag(number = 8)
        public BACnetNotifyType notify_type;

        @SerializationTag(number = 9)
        public Optional<Boolean> ack_required;

        @SerializationTag(number = 10)
        public Optional<BACnetEventState> from_state;

        @SerializationTag(number = 11)
        public BACnetEventState to_state;

        @SerializationTag(number = 12)
        public Optional<BACnetNotificationParameters> event_values;
    }
}
