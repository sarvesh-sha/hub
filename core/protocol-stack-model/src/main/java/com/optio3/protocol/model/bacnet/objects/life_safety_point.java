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
import com.optio3.protocol.model.bacnet.constructed.BACnetDeviceObjectReference;
import com.optio3.protocol.model.bacnet.constructed.BACnetNameValue;
import com.optio3.protocol.model.bacnet.constructed.BACnetObjectPropertyReference;
import com.optio3.protocol.model.bacnet.constructed.choice.BACnetTimeStamp;
import com.optio3.protocol.model.bacnet.constructed.choice.BACnetValueSource;
import com.optio3.protocol.model.bacnet.enums.BACnetEngineeringUnits;
import com.optio3.protocol.model.bacnet.enums.BACnetEventState;
import com.optio3.protocol.model.bacnet.enums.BACnetLifeSafetyMode;
import com.optio3.protocol.model.bacnet.enums.BACnetLifeSafetyOperation;
import com.optio3.protocol.model.bacnet.enums.BACnetLifeSafetyState;
import com.optio3.protocol.model.bacnet.enums.BACnetMaintenance;
import com.optio3.protocol.model.bacnet.enums.BACnetNotifyType;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectTypeOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetReliability;
import com.optio3.protocol.model.bacnet.enums.BACnetSilencedState;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetEventTransitionBits;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetStatusFlags;

// NOTE: Generated automatically by regenerateObjectModels unit test!!
@JsonTypeName("BACnet:life_safety_point")
public final class life_safety_point extends BACnetObjectModel
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
    public List<BACnetLifeSafetyMode>          accepted_modes;
    public BACnetEventState                    event_state;
    public String                              object_name;
    public BACnetLifeSafetyOperation           operation_expected;
    public boolean                             out_of_service;
    public BACnetLifeSafetyState               present_value;
    public BACnetPropertyIdentifierOrUnknown[] property_list;
    public BACnetReliability                   reliability;
    public BACnetSilencedState                 silenced;
    public BACnetStatusFlags                   status_flags;
    public BACnetLifeSafetyState               tracking_value;

    //////////////////////////////
    //
    // RequiredAndWritable fields:
    //
    public BACnetLifeSafetyMode                mode;

    ///////////////////
    //
    // Optional fields:
    //
    public BACnetEventTransitionBits           acked_transitions;
    public List<BACnetLifeSafetyState>         alarm_values;
    public String                              description;
    public String                              device_type;
    public float                               direct_reading;
    public boolean                             event_algorithm_inhibit;
    public BACnetObjectPropertyReference       event_algorithm_inhibit_ref;
    public boolean                             event_detection_enable;
    public BACnetEventTransitionBits           event_enable;
    public String[]                            event_message_texts;
    public String[]                            event_message_texts_config;
    public BACnetTimeStamp[]                   event_time_stamps;
    public List<BACnetLifeSafetyState>         fault_values;
    public List<BACnetLifeSafetyState>         life_safety_alarm_values;
    public BACnetMaintenance                   maintenance_required;
    public List<BACnetDeviceObjectReference>   member_of;
    public long                                notification_class;
    public BACnetNotifyType                    notify_type;
    public String                              profile_location;
    public String                              profile_name;
    public boolean                             reliability_evaluation_inhibit;
    public long                                setting;
    public BACnetNameValue[]                   tags;
    public long                                time_delay;
    public long                                time_delay_normal;
    public BACnetEngineeringUnits              units;
    public BACnetValueSource                   value_source;
    // @formatter:on

    public life_safety_point()
    {
        super(BACnetObjectType.life_safety_point);
        accepted_modes = Lists.newArrayList();
        alarm_values = Lists.newArrayList();
        event_message_texts = new String[3];
        event_message_texts_config = new String[3];
        event_time_stamps = new BACnetTimeStamp[3];
        event_time_stamps[0] = new BACnetTimeStamp();
        event_time_stamps[1] = new BACnetTimeStamp();
        event_time_stamps[2] = new BACnetTimeStamp();
        fault_values = Lists.newArrayList();
        life_safety_alarm_values = Lists.newArrayList();
        member_of = Lists.newArrayList();
    }
}
