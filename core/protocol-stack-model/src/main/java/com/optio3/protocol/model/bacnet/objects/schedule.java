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
import com.optio3.protocol.model.bacnet.AnyValue;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.BACnetObjectModel;
import com.optio3.protocol.model.bacnet.constructed.BACnetDailySchedule;
import com.optio3.protocol.model.bacnet.constructed.BACnetDateRange;
import com.optio3.protocol.model.bacnet.constructed.BACnetDeviceObjectPropertyReference;
import com.optio3.protocol.model.bacnet.constructed.BACnetNameValue;
import com.optio3.protocol.model.bacnet.constructed.BACnetSpecialEvent;
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
@JsonTypeName("BACnet:schedule")
public final class schedule extends BACnetObjectModel
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
    public BACnetDateRange                           effective_period;
    public List<BACnetDeviceObjectPropertyReference> list_of_object_property_references;
    public String                                    object_name;
    public boolean                                   out_of_service;
    public AnyValue                                  present_value;
    public long                                      priority_for_writing;
    public BACnetPropertyIdentifierOrUnknown[]       property_list;
    public BACnetReliability                         reliability;
    public AnyValue                                  schedule_default;
    public BACnetStatusFlags                         status_flags;

    ///////////////////
    //
    // Optional fields:
    //
    public BACnetEventTransitionBits                 acked_transitions;
    public String                                    description;
    public boolean                                   event_detection_enable;
    public BACnetEventTransitionBits                 event_enable;
    public String[]                                  event_message_texts;
    public String[]                                  event_message_texts_config;
    public BACnetEventState                          event_state;
    public BACnetTimeStamp[]                         event_time_stamps;
    public BACnetSpecialEvent[]                      exception_schedule;
    public long                                      notification_class;
    public BACnetNotifyType                          notify_type;
    public String                                    profile_location;
    public String                                    profile_name;
    public boolean                                   reliability_evaluation_inhibit;
    public BACnetNameValue[]                         tags;
    public BACnetDailySchedule[]                     weekly_schedule;
    // @formatter:on

    public schedule()
    {
        super(BACnetObjectType.schedule);
        event_message_texts = new String[3];
        event_message_texts_config = new String[3];
        event_time_stamps = new BACnetTimeStamp[3];
        event_time_stamps[0] = new BACnetTimeStamp();
        event_time_stamps[1] = new BACnetTimeStamp();
        event_time_stamps[2] = new BACnetTimeStamp();
        list_of_object_property_references = Lists.newArrayList();
        weekly_schedule = new BACnetDailySchedule[7];
        weekly_schedule[0] = new BACnetDailySchedule();
        weekly_schedule[1] = new BACnetDailySchedule();
        weekly_schedule[2] = new BACnetDailySchedule();
        weekly_schedule[3] = new BACnetDailySchedule();
        weekly_schedule[4] = new BACnetDailySchedule();
        weekly_schedule[5] = new BACnetDailySchedule();
        weekly_schedule[6] = new BACnetDailySchedule();
    }
}
