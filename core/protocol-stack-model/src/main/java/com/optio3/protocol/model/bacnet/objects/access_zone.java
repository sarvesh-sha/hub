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
import com.optio3.protocol.model.bacnet.constructed.BACnetDeviceObjectReference;
import com.optio3.protocol.model.bacnet.constructed.BACnetNameValue;
import com.optio3.protocol.model.bacnet.constructed.BACnetObjectPropertyReference;
import com.optio3.protocol.model.bacnet.constructed.choice.BACnetTimeStamp;
import com.optio3.protocol.model.bacnet.enums.BACnetAccessPassbackMode;
import com.optio3.protocol.model.bacnet.enums.BACnetAccessZoneOccupancyState;
import com.optio3.protocol.model.bacnet.enums.BACnetEventState;
import com.optio3.protocol.model.bacnet.enums.BACnetNotifyType;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectTypeOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetReliability;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetEventTransitionBits;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetStatusFlags;

// NOTE: Generated automatically by regenerateObjectModels unit test!!
@JsonTypeName("BACnet:access_zone")
public final class access_zone extends BACnetObjectModel
{
    // @formatter:off
    @JsonIgnore // Avoid serializing the identity, it would be a duplicate in most cases.
    public BACnetObjectIdentifier               object_identifier;

    @JsonIgnore // Avoid serializing the type, we already know it.
    public BACnetObjectTypeOrUnknown            object_type;

    ///////////////////
    //
    // Required fields:
    //
    public List<BACnetDeviceObjectReference>    entry_points;
    public BACnetEventState                     event_state;
    public List<BACnetDeviceObjectReference>    exit_points;
    public String                               object_name;
    public BACnetAccessZoneOccupancyState       occupancy_state;
    public boolean                              out_of_service;
    public BACnetPropertyIdentifierOrUnknown[]  property_list;
    public BACnetReliability                    reliability;
    public BACnetStatusFlags                    status_flags;

    //////////////////////////////
    //
    // RequiredAndWritable fields:
    //
    public long                                 global_identifier;

    ///////////////////
    //
    // Optional fields:
    //
    public BACnetEventTransitionBits            acked_transitions;
    public int                                  adjust_value;
    public List<BACnetAccessZoneOccupancyState> alarm_values;
    public List<BACnetDeviceObjectReference>    credentials_in_zone;
    public String                               description;
    public boolean                              event_algorithm_inhibit;
    public BACnetObjectPropertyReference        event_algorithm_inhibit_ref;
    public boolean                              event_detection_enable;
    public BACnetEventTransitionBits            event_enable;
    public String[]                             event_message_texts;
    public String[]                             event_message_texts_config;
    public BACnetTimeStamp[]                    event_time_stamps;
    public BACnetDeviceObjectReference          last_credential_added;
    public BACnetDateTime                       last_credential_added_time;
    public BACnetDeviceObjectReference          last_credential_removed;
    public BACnetDateTime                       last_credential_removed_time;
    public long                                 notification_class;
    public BACnetNotifyType                     notify_type;
    public long                                 occupancy_count;
    public boolean                              occupancy_count_enable;
    public long                                 occupancy_lower_limit;
    public long                                 occupancy_upper_limit;
    public BACnetAccessPassbackMode             passback_mode;
    public long                                 passback_timeout;
    public String                               profile_location;
    public String                               profile_name;
    public boolean                              reliability_evaluation_inhibit;
    public BACnetNameValue[]                    tags;
    public long                                 time_delay;
    public long                                 time_delay_normal;
    // @formatter:on

    public access_zone()
    {
        super(BACnetObjectType.access_zone);
        alarm_values = Lists.newArrayList();
        credentials_in_zone = Lists.newArrayList();
        entry_points = Lists.newArrayList();
        event_message_texts = new String[3];
        event_message_texts_config = new String[3];
        event_time_stamps = new BACnetTimeStamp[3];
        event_time_stamps[0] = new BACnetTimeStamp();
        event_time_stamps[1] = new BACnetTimeStamp();
        event_time_stamps[2] = new BACnetTimeStamp();
        exit_points = Lists.newArrayList();
    }
}
