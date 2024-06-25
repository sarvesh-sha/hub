/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.objects;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.lang.Unsigned32;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.BACnetObjectModel;
import com.optio3.protocol.model.bacnet.constructed.BACnetDateTime;
import com.optio3.protocol.model.bacnet.constructed.BACnetNameValue;
import com.optio3.protocol.model.bacnet.constructed.BACnetObjectPropertyReference;
import com.optio3.protocol.model.bacnet.constructed.BACnetPriorityArray;
import com.optio3.protocol.model.bacnet.constructed.choice.BACnetTimeStamp;
import com.optio3.protocol.model.bacnet.constructed.choice.BACnetValueSource;
import com.optio3.protocol.model.bacnet.enums.BACnetBinaryPV;
import com.optio3.protocol.model.bacnet.enums.BACnetEventState;
import com.optio3.protocol.model.bacnet.enums.BACnetNotifyType;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectTypeOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetPolarity;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetReliability;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetEventTransitionBits;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetStatusFlags;

// NOTE: Generated automatically by regenerateObjectModels unit test!!
@JsonTypeName("BACnet:binary_output")
public final class binary_output extends BACnetObjectModel
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
    public Optional<Unsigned32>                current_command_priority;
    public BACnetEventState                    event_state;
    public String                              object_name;
    public boolean                             out_of_service;
    public BACnetPolarity                      polarity;
    public BACnetPriorityArray                 priority_array;
    public BACnetPropertyIdentifierOrUnknown[] property_list;
    public BACnetBinaryPV                      relinquish_default;
    public BACnetStatusFlags                   status_flags;

    //////////////////////////////
    //
    // RequiredAndWritable fields:
    //
    public BACnetBinaryPV                      present_value;

    ///////////////////
    //
    // Optional fields:
    //
    public BACnetEventTransitionBits           acked_transitions;
    public String                              active_text;
    public long                                change_of_state_count;
    public BACnetDateTime                      change_of_state_time;
    public BACnetTimeStamp[]                   command_time_array;
    public String                              description;
    public String                              device_type;
    public long                                elapsed_active_time;
    public boolean                             event_algorithm_inhibit;
    public BACnetObjectPropertyReference       event_algorithm_inhibit_ref;
    public boolean                             event_detection_enable;
    public BACnetEventTransitionBits           event_enable;
    public String[]                            event_message_texts;
    public String[]                            event_message_texts_config;
    public BACnetTimeStamp[]                   event_time_stamps;
    public BACnetBinaryPV                      feedback_value;
    public String                              inactive_text;
    public Optional<BACnetBinaryPV>            interface_value;
    public BACnetTimeStamp                     last_command_time;
    public long                                minimum_off_time;
    public long                                minimum_on_time;
    public long                                notification_class;
    public BACnetNotifyType                    notify_type;
    public String                              profile_location;
    public String                              profile_name;
    public BACnetReliability                   reliability;
    public boolean                             reliability_evaluation_inhibit;
    public BACnetNameValue[]                   tags;
    public long                                time_delay;
    public long                                time_delay_normal;
    public BACnetDateTime                      time_of_active_time_reset;
    public BACnetDateTime                      time_of_state_count_reset;
    public BACnetValueSource                   value_source;
    public BACnetValueSource[]                 value_source_array;
    // @formatter:on

    public binary_output()
    {
        super(BACnetObjectType.binary_output);
        command_time_array = new BACnetTimeStamp[16];
        command_time_array[0] = new BACnetTimeStamp();
        command_time_array[1] = new BACnetTimeStamp();
        command_time_array[2] = new BACnetTimeStamp();
        command_time_array[3] = new BACnetTimeStamp();
        command_time_array[4] = new BACnetTimeStamp();
        command_time_array[5] = new BACnetTimeStamp();
        command_time_array[6] = new BACnetTimeStamp();
        command_time_array[7] = new BACnetTimeStamp();
        command_time_array[8] = new BACnetTimeStamp();
        command_time_array[9] = new BACnetTimeStamp();
        command_time_array[10] = new BACnetTimeStamp();
        command_time_array[11] = new BACnetTimeStamp();
        command_time_array[12] = new BACnetTimeStamp();
        command_time_array[13] = new BACnetTimeStamp();
        command_time_array[14] = new BACnetTimeStamp();
        command_time_array[15] = new BACnetTimeStamp();
        event_message_texts = new String[3];
        event_message_texts_config = new String[3];
        event_time_stamps = new BACnetTimeStamp[3];
        event_time_stamps[0] = new BACnetTimeStamp();
        event_time_stamps[1] = new BACnetTimeStamp();
        event_time_stamps[2] = new BACnetTimeStamp();
        value_source_array = new BACnetValueSource[16];
        value_source_array[0] = new BACnetValueSource();
        value_source_array[1] = new BACnetValueSource();
        value_source_array[2] = new BACnetValueSource();
        value_source_array[3] = new BACnetValueSource();
        value_source_array[4] = new BACnetValueSource();
        value_source_array[5] = new BACnetValueSource();
        value_source_array[6] = new BACnetValueSource();
        value_source_array[7] = new BACnetValueSource();
        value_source_array[8] = new BACnetValueSource();
        value_source_array[9] = new BACnetValueSource();
        value_source_array[10] = new BACnetValueSource();
        value_source_array[11] = new BACnetValueSource();
        value_source_array[12] = new BACnetValueSource();
        value_source_array[13] = new BACnetValueSource();
        value_source_array[14] = new BACnetValueSource();
        value_source_array[15] = new BACnetValueSource();
    }
}
