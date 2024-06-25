/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.objects;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;
import com.optio3.lang.Unsigned32;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.BACnetObjectModel;
import com.optio3.protocol.model.bacnet.constructed.BACnetDeviceObjectReference;
import com.optio3.protocol.model.bacnet.constructed.BACnetNameValue;
import com.optio3.protocol.model.bacnet.constructed.BACnetObjectPropertyReference;
import com.optio3.protocol.model.bacnet.constructed.BACnetPriorityArray;
import com.optio3.protocol.model.bacnet.constructed.choice.BACnetTimeStamp;
import com.optio3.protocol.model.bacnet.constructed.choice.BACnetValueSource;
import com.optio3.protocol.model.bacnet.enums.BACnetDoorAlarmState;
import com.optio3.protocol.model.bacnet.enums.BACnetDoorSecuredStatus;
import com.optio3.protocol.model.bacnet.enums.BACnetDoorStatus;
import com.optio3.protocol.model.bacnet.enums.BACnetDoorValue;
import com.optio3.protocol.model.bacnet.enums.BACnetEventState;
import com.optio3.protocol.model.bacnet.enums.BACnetLockStatus;
import com.optio3.protocol.model.bacnet.enums.BACnetMaintenance;
import com.optio3.protocol.model.bacnet.enums.BACnetNotifyType;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectTypeOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetReliability;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetEventTransitionBits;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetStatusFlags;

// NOTE: Generated automatically by regenerateObjectModels unit test!!
@JsonTypeName("BACnet:access_door")
public final class access_door extends BACnetObjectModel
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
    public long                                door_extended_pulse_time;
    public long                                door_open_too_long_time;
    public long                                door_pulse_time;
    public BACnetEventState                    event_state;
    public String                              object_name;
    public boolean                             out_of_service;
    public BACnetPriorityArray                 priority_array;
    public BACnetPropertyIdentifierOrUnknown[] property_list;
    public BACnetReliability                   reliability;
    public BACnetDoorValue                     relinquish_default;
    public BACnetStatusFlags                   status_flags;

    //////////////////////////////
    //
    // RequiredAndWritable fields:
    //
    public BACnetDoorValue                     present_value;

    ///////////////////
    //
    // Optional fields:
    //
    public BACnetEventTransitionBits           acked_transitions;
    public List<BACnetDoorAlarmState>          alarm_values;
    public BACnetTimeStamp[]                   command_time_array;
    public String                              description;
    public BACnetDoorAlarmState                door_alarm_state;
    public BACnetDeviceObjectReference[]       door_members;
    public BACnetDoorStatus                    door_status;
    public long                                door_unlock_delay_time;
    public boolean                             event_algorithm_inhibit;
    public BACnetObjectPropertyReference       event_algorithm_inhibit_ref;
    public boolean                             event_detection_enable;
    public BACnetEventTransitionBits           event_enable;
    public String[]                            event_message_texts;
    public String[]                            event_message_texts_config;
    public BACnetTimeStamp[]                   event_time_stamps;
    public List<BACnetDoorAlarmState>          fault_values;
    public BACnetTimeStamp                     last_command_time;
    public BACnetLockStatus                    lock_status;
    public BACnetMaintenance                   maintenance_required;
    public List<BACnetDoorAlarmState>          masked_alarm_values;
    public long                                notification_class;
    public BACnetNotifyType                    notify_type;
    public String                              profile_location;
    public String                              profile_name;
    public boolean                             reliability_evaluation_inhibit;
    public BACnetDoorSecuredStatus             secured_status;
    public BACnetNameValue[]                   tags;
    public long                                time_delay;
    public long                                time_delay_normal;
    public BACnetValueSource                   value_source;
    public BACnetValueSource[]                 value_source_array;
    // @formatter:on

    public access_door()
    {
        super(BACnetObjectType.access_door);
        alarm_values = Lists.newArrayList();
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
        fault_values = Lists.newArrayList();
        masked_alarm_values = Lists.newArrayList();
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