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
import com.optio3.protocol.model.bacnet.constructed.BACnetAssignedLandingCalls;
import com.optio3.protocol.model.bacnet.constructed.BACnetDeviceObjectReference;
import com.optio3.protocol.model.bacnet.constructed.BACnetLandingDoorStatus;
import com.optio3.protocol.model.bacnet.constructed.BACnetLiftCarCallList;
import com.optio3.protocol.model.bacnet.constructed.BACnetNameValue;
import com.optio3.protocol.model.bacnet.constructed.BACnetObjectPropertyReference;
import com.optio3.protocol.model.bacnet.constructed.choice.BACnetTimeStamp;
import com.optio3.protocol.model.bacnet.enums.BACnetDoorStatus;
import com.optio3.protocol.model.bacnet.enums.BACnetEngineeringUnits;
import com.optio3.protocol.model.bacnet.enums.BACnetEventState;
import com.optio3.protocol.model.bacnet.enums.BACnetLiftCarDirection;
import com.optio3.protocol.model.bacnet.enums.BACnetLiftCarDoorCommand;
import com.optio3.protocol.model.bacnet.enums.BACnetLiftCarDriveStatus;
import com.optio3.protocol.model.bacnet.enums.BACnetLiftCarMode;
import com.optio3.protocol.model.bacnet.enums.BACnetLiftFault;
import com.optio3.protocol.model.bacnet.enums.BACnetNotifyType;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectTypeOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetReliability;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetEventTransitionBits;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetStatusFlags;

// NOTE: Generated automatically by regenerateObjectModels unit test!!
@JsonTypeName("BACnet:lift")
public final class lift extends BACnetObjectModel
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
    public BACnetDoorStatus[]                  car_door_status;
    public BACnetLiftCarDirection              car_moving_direction;
    public long                                car_position;
    public BACnetObjectIdentifier              elevator_group;
    public List<BACnetLiftFault>               fault_signals;
    public long                                group_id;
    public long                                installation_id;
    public String                              object_name;
    public boolean                             out_of_service;
    public boolean                             passenger_alarm;
    public BACnetPropertyIdentifierOrUnknown[] property_list;
    public BACnetStatusFlags                   status_flags;

    ///////////////////
    //
    // Optional fields:
    //
    public BACnetEventTransitionBits           acked_transitions;
    public BACnetAssignedLandingCalls[]        assigned_landing_calls;
    public BACnetLiftCarDirection              car_assigned_direction;
    public BACnetLiftCarDoorCommand[]          car_door_command;
    public String[]                            car_door_text;
    public boolean                             car_door_zone;
    public BACnetLiftCarDriveStatus            car_drive_status;
    public float                               car_load;
    public BACnetEngineeringUnits              car_load_units;
    public BACnetLiftCarMode                   car_mode;
    public String                              description;
    public float                               energy_meter;
    public BACnetDeviceObjectReference         energy_meter_ref;
    public boolean                             event_algorithm_inhibit;
    public BACnetObjectPropertyReference       event_algorithm_inhibit_ref;
    public boolean                             event_detection_enable;
    public BACnetEventTransitionBits           event_enable;
    public String[]                            event_message_texts;
    public String[]                            event_message_texts_config;
    public BACnetEventState                    event_state;
    public BACnetTimeStamp[]                   event_time_stamps;
    public String[]                            floor_text;
    public BACnetObjectIdentifier              higher_deck;
    public BACnetLandingDoorStatus[]           landing_door_status;
    public BACnetObjectIdentifier              lower_deck;
    public long[]                              making_car_call;
    public long                                next_stopping_floor;
    public long                                notification_class;
    public BACnetNotifyType                    notify_type;
    public String                              profile_location;
    public String                              profile_name;
    public BACnetLiftCarCallList[]             registered_car_call;
    public BACnetReliability                   reliability;
    public boolean                             reliability_evaluation_inhibit;
    public BACnetNameValue[]                   tags;
    public long                                time_delay;
    public long                                time_delay_normal;
    // @formatter:on

    public lift()
    {
        super(BACnetObjectType.lift);
        event_message_texts = new String[3];
        event_message_texts_config = new String[3];
        event_time_stamps = new BACnetTimeStamp[3];
        event_time_stamps[0] = new BACnetTimeStamp();
        event_time_stamps[1] = new BACnetTimeStamp();
        event_time_stamps[2] = new BACnetTimeStamp();
        fault_signals = Lists.newArrayList();
    }
}
