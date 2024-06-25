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
import com.optio3.protocol.model.bacnet.constructed.BACnetAuthenticationFactor;
import com.optio3.protocol.model.bacnet.constructed.BACnetAuthenticationPolicy;
import com.optio3.protocol.model.bacnet.constructed.BACnetDeviceObjectReference;
import com.optio3.protocol.model.bacnet.constructed.BACnetNameValue;
import com.optio3.protocol.model.bacnet.constructed.BACnetObjectPropertyReference;
import com.optio3.protocol.model.bacnet.constructed.choice.BACnetTimeStamp;
import com.optio3.protocol.model.bacnet.enums.BACnetAccessEvent;
import com.optio3.protocol.model.bacnet.enums.BACnetAuthenticationStatus;
import com.optio3.protocol.model.bacnet.enums.BACnetAuthorizationMode;
import com.optio3.protocol.model.bacnet.enums.BACnetEventState;
import com.optio3.protocol.model.bacnet.enums.BACnetNotifyType;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectTypeOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetReliability;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetEventTransitionBits;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetStatusFlags;

// NOTE: Generated automatically by regenerateObjectModels unit test!!
@JsonTypeName("BACnet:access_point")
public final class access_point extends BACnetObjectModel
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
    public BACnetDeviceObjectReference[]       access_doors;
    public BACnetAccessEvent                   access_event;
    public BACnetDeviceObjectReference         access_event_credential;
    public long                                access_event_tag;
    public BACnetTimeStamp                     access_event_time;
    public long                                active_authentication_policy;
    public BACnetAuthenticationStatus          authentication_status;
    public BACnetAuthorizationMode             authorization_mode;
    public BACnetEventState                    event_state;
    public long                                number_of_authentication_policies;
    public String                              object_name;
    public boolean                             out_of_service;
    public long                                priority_for_writing;
    public BACnetPropertyIdentifierOrUnknown[] property_list;
    public BACnetReliability                   reliability;
    public BACnetStatusFlags                   status_flags;

    ///////////////////
    //
    // Optional fields:
    //
    public List<BACnetAccessEvent>             access_alarm_events;
    public BACnetAuthenticationFactor          access_event_authentication_factor;
    public List<BACnetAccessEvent>             access_transaction_events;
    public long                                accompaniment_time;
    public BACnetEventTransitionBits           acked_transitions;
    public BACnetAuthenticationPolicy[]        authentication_policy_list;
    public String[]                            authentication_policy_names;
    public String                              description;
    public boolean                             event_algorithm_inhibit;
    public BACnetObjectPropertyReference       event_algorithm_inhibit_ref;
    public boolean                             event_detection_enable;
    public BACnetEventTransitionBits           event_enable;
    public String[]                            event_message_texts;
    public String[]                            event_message_texts_config;
    public BACnetTimeStamp[]                   event_time_stamps;
    public List<BACnetAccessEvent>             failed_attempt_events;
    public long                                failed_attempts;
    public long                                failed_attempts_time;
    public boolean                             lockout;
    public long                                lockout_relinquish_time;
    public long                                max_failed_attempts;
    public boolean                             muster_point;
    public long                                notification_class;
    public BACnetNotifyType                    notify_type;
    public boolean                             occupancy_count_adjust;
    public boolean                             occupancy_lower_limit_enforced;
    public boolean                             occupancy_upper_limit_enforced;
    public String                              profile_location;
    public String                              profile_name;
    public boolean                             reliability_evaluation_inhibit;
    public BACnetNameValue[]                   tags;
    public byte                                threat_level;
    public long                                transaction_notification_class;
    public long                                verification_time;
    public BACnetDeviceObjectReference         zone_from;
    public BACnetDeviceObjectReference         zone_to;
    // @formatter:on

    public access_point()
    {
        super(BACnetObjectType.access_point);
        access_alarm_events = Lists.newArrayList();
        access_transaction_events = Lists.newArrayList();
        event_message_texts = new String[3];
        event_message_texts_config = new String[3];
        event_time_stamps = new BACnetTimeStamp[3];
        event_time_stamps[0] = new BACnetTimeStamp();
        event_time_stamps[1] = new BACnetTimeStamp();
        event_time_stamps[2] = new BACnetTimeStamp();
        failed_attempt_events = Lists.newArrayList();
    }
}
