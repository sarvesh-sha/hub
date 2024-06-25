/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.objects;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.BACnetObjectModel;
import com.optio3.protocol.model.bacnet.constructed.BACnetDateTime;
import com.optio3.protocol.model.bacnet.constructed.BACnetEventLogRecord;
import com.optio3.protocol.model.bacnet.constructed.BACnetNameValue;
import com.optio3.protocol.model.bacnet.constructed.BACnetObjectPropertyReference;
import com.optio3.protocol.model.bacnet.constructed.choice.BACnetTimeStamp;
import com.optio3.protocol.model.bacnet.enums.BACnetEventState;
import com.optio3.protocol.model.bacnet.enums.BACnetNotifyType;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectTypeOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetReliability;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetEventTransitionBits;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetStatusFlags;

// NOTE: Generated automatically by regenerateObjectModels unit test!!
@JsonTypeName("BACnet:event_log")
public final class event_log extends BACnetObjectModel
{
    // @formatter:off
    @JsonIgnore // Avoid serializing the identity, it would be a duplicate in most cases.
    public BACnetObjectIdentifier              object_identifier;

    @JsonIgnore // Avoid serializing the type, we already know it.
    public BACnetObjectTypeOrUnknown           object_type;

    ///////////////////
    //
    // Required fields:
    //
    public long                                buffer_size;
    public BACnetEventState                    event_state;
    public List<BACnetEventLogRecord>          log_buffer;
    public String                              object_name;
    public BACnetPropertyIdentifierOrUnknown[] property_list;
    public BACnetStatusFlags                   status_flags;
    public boolean                             stop_when_full;
    public long                                total_record_count;

    //////////////////////////////
    //
    // RequiredAndWritable fields:
    //
    public boolean                             enable;
    public long                                record_count;

    ///////////////////
    //
    // Optional fields:
    //
    public BACnetEventTransitionBits           acked_transitions;
    public String                              description;
    public boolean                             event_algorithm_inhibit;
    public BACnetObjectPropertyReference       event_algorithm_inhibit_ref;
    public boolean                             event_detection_enable;
    public BACnetEventTransitionBits           event_enable;
    public String[]                            event_message_texts;
    public String[]                            event_message_texts_config;
    public BACnetTimeStamp[]                   event_time_stamps;
    public long                                last_notify_record;
    public long                                notification_class;
    public long                                notification_threshold;
    public BACnetNotifyType                    notify_type;
    public String                              profile_location;
    public String                              profile_name;
    public long                                records_since_notification;
    public BACnetReliability                   reliability;
    public boolean                             reliability_evaluation_inhibit;
    public BACnetDateTime                      start_time;
    public BACnetDateTime                      stop_time;
    public BACnetNameValue[]                   tags;
    // @formatter:on

    public event_log()
    {
        super(BACnetObjectType.event_log);
        event_message_texts = new String[3];
        event_message_texts_config = new String[3];
        event_time_stamps = new BACnetTimeStamp[3];
        event_time_stamps[0] = new BACnetTimeStamp();
        event_time_stamps[1] = new BACnetTimeStamp();
        event_time_stamps[2] = new BACnetTimeStamp();
        log_buffer = Lists.newArrayList();
    }
}
