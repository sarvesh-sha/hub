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
import com.optio3.protocol.model.bacnet.constructed.BACnetDeviceObjectPropertyReference;
import com.optio3.protocol.model.bacnet.constructed.BACnetNameValue;
import com.optio3.protocol.model.bacnet.constructed.BACnetObjectPropertyReference;
import com.optio3.protocol.model.bacnet.constructed.choice.BACnetTimeStamp;
import com.optio3.protocol.model.bacnet.constructed.choice.BACnetTimerStateChangeValue;
import com.optio3.protocol.model.bacnet.enums.BACnetEventState;
import com.optio3.protocol.model.bacnet.enums.BACnetNotifyType;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectTypeOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetReliability;
import com.optio3.protocol.model.bacnet.enums.BACnetTimerState;
import com.optio3.protocol.model.bacnet.enums.BACnetTimerTransition;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetEventTransitionBits;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetStatusFlags;

// NOTE: Generated automatically by regenerateObjectModels unit test!!
@JsonTypeName("BACnet:timer")
public final class timer extends BACnetObjectModel
{
    // @formatter:off
    @JsonIgnore // Avoid serializing the identity, it would be a duplicate in most cases.
    public BACnetObjectIdentifier                    object_identifier;

    @JsonIgnore // Avoid serializing the type, we already know it.
    public BACnetObjectTypeOrUnknown                 object_type;

    ///////////////////
    //
    // Required fields:
    //
    public String                                    object_name;
    public long                                      present_value;
    public BACnetPropertyIdentifierOrUnknown[]       property_list;
    public BACnetStatusFlags                         status_flags;
    public boolean                                   timer_running;
    public BACnetTimerState                          timer_state;

    ///////////////////
    //
    // Optional fields:
    //
    public BACnetEventTransitionBits                 acked_transitions;
    public List<BACnetTimerState>                    alarm_values;
    public long                                      default_timeout;
    public String                                    description;
    public boolean                                   event_algorithm_inhibit;
    public BACnetObjectPropertyReference             event_algorithm_inhibit_ref;
    public boolean                                   event_detection_enable;
    public BACnetEventTransitionBits                 event_enable;
    public String[]                                  event_message_texts;
    public String[]                                  event_message_texts_config;
    public BACnetEventState                          event_state;
    public BACnetTimeStamp[]                         event_time_stamps;
    public BACnetDateTime                            expiration_time;
    public long                                      initial_timeout;
    public BACnetTimerTransition                     last_state_change;
    public List<BACnetDeviceObjectPropertyReference> list_of_object_property_references;
    public long                                      max_pres_value;
    public long                                      min_pres_value;
    public long                                      notification_class;
    public BACnetNotifyType                          notify_type;
    public boolean                                   out_of_service;
    public long                                      priority_for_writing;
    public String                                    profile_location;
    public String                                    profile_name;
    public BACnetReliability                         reliability;
    public boolean                                   reliability_evaluation_inhibit;
    public long                                      resolution;
    public BACnetTimerStateChangeValue[]             state_change_values;
    public BACnetNameValue[]                         tags;
    public long                                      time_delay;
    public long                                      time_delay_normal;
    public BACnetDateTime                            update_time;
    // @formatter:on

    public timer()
    {
        super(BACnetObjectType.timer);
        alarm_values = Lists.newArrayList();
        event_message_texts = new String[3];
        event_message_texts_config = new String[3];
        event_time_stamps = new BACnetTimeStamp[3];
        event_time_stamps[0] = new BACnetTimeStamp();
        event_time_stamps[1] = new BACnetTimeStamp();
        event_time_stamps[2] = new BACnetTimeStamp();
        list_of_object_property_references = Lists.newArrayList();
        state_change_values = new BACnetTimerStateChangeValue[7];
        state_change_values[0] = new BACnetTimerStateChangeValue();
        state_change_values[1] = new BACnetTimerStateChangeValue();
        state_change_values[2] = new BACnetTimerStateChangeValue();
        state_change_values[3] = new BACnetTimerStateChangeValue();
        state_change_values[4] = new BACnetTimerStateChangeValue();
        state_change_values[5] = new BACnetTimerStateChangeValue();
        state_change_values[6] = new BACnetTimerStateChangeValue();
    }
}
