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
import com.optio3.protocol.model.bacnet.constructed.BACnetDeviceObjectPropertyReference;
import com.optio3.protocol.model.bacnet.constructed.BACnetNameValue;
import com.optio3.protocol.model.bacnet.constructed.BACnetObjectPropertyReference;
import com.optio3.protocol.model.bacnet.constructed.BACnetPropertyAccessResult;
import com.optio3.protocol.model.bacnet.constructed.choice.BACnetClientCOV;
import com.optio3.protocol.model.bacnet.constructed.choice.BACnetRecipient;
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
@JsonTypeName("BACnet:global_group")
public final class global_group extends BACnetObjectModel
{
    // @formatter:off
    @JsonIgnore // Avoid serializing the identity, it would be a duplicate in most cases.
    public BACnetObjectIdentifier                object_identifier;

    @JsonIgnore // Avoid serializing the type, we already know it.
    public BACnetObjectTypeOrUnknown             object_type;

    ///////////////////
    //
    // Required fields:
    //
    public BACnetEventState                      event_state;
    public BACnetDeviceObjectPropertyReference[] group_members;
    public BACnetStatusFlags                     member_status_flags;
    public String                                object_name;
    public boolean                               out_of_service;
    public BACnetPropertyAccessResult[]          present_value;
    public BACnetPropertyIdentifierOrUnknown[]   property_list;
    public BACnetStatusFlags                     status_flags;

    ///////////////////
    //
    // Optional fields:
    //
    public BACnetEventTransitionBits             acked_transitions;
    public BACnetClientCOV                       client_cov_increment;
    public long                                  cov_resubscription_interval;
    public long                                  covu_period;
    public List<BACnetRecipient>                 covu_recipients;
    public String                                description;
    public boolean                               event_algorithm_inhibit;
    public BACnetObjectPropertyReference         event_algorithm_inhibit_ref;
    public boolean                               event_detection_enable;
    public BACnetEventTransitionBits             event_enable;
    public String[]                              event_message_texts;
    public String[]                              event_message_texts_config;
    public BACnetTimeStamp[]                     event_time_stamps;
    public String[]                              group_member_names;
    public long                                  notification_class;
    public BACnetNotifyType                      notify_type;
    public String                                profile_location;
    public String                                profile_name;
    public BACnetReliability                     reliability;
    public boolean                               reliability_evaluation_inhibit;
    public long                                  requested_update_interval;
    public BACnetNameValue[]                     tags;
    public long                                  time_delay;
    public long                                  time_delay_normal;
    public long                                  update_interval;
    // @formatter:on

    public global_group()
    {
        super(BACnetObjectType.global_group);
        covu_recipients = Lists.newArrayList();
        event_message_texts = new String[3];
        event_message_texts_config = new String[3];
        event_time_stamps = new BACnetTimeStamp[3];
        event_time_stamps[0] = new BACnetTimeStamp();
        event_time_stamps[1] = new BACnetTimeStamp();
        event_time_stamps[2] = new BACnetTimeStamp();
    }
}
