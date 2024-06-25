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
import com.optio3.protocol.model.bacnet.constructed.BACnetLightingCommand;
import com.optio3.protocol.model.bacnet.constructed.BACnetNameValue;
import com.optio3.protocol.model.bacnet.constructed.BACnetPriorityArray;
import com.optio3.protocol.model.bacnet.constructed.choice.BACnetTimeStamp;
import com.optio3.protocol.model.bacnet.constructed.choice.BACnetValueSource;
import com.optio3.protocol.model.bacnet.enums.BACnetLightingInProgress;
import com.optio3.protocol.model.bacnet.enums.BACnetLightingTransition;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectTypeOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetReliability;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetStatusFlags;

// NOTE: Generated automatically by regenerateObjectModels unit test!!
@JsonTypeName("BACnet:lighting_output")
public final class lighting_output extends BACnetObjectModel
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
    public boolean                             blink_warn_enable;
    public Optional<Unsigned32>                current_command_priority;
    public long                                default_fade_time;
    public float                               default_ramp_rate;
    public float                               default_step_increment;
    public boolean                             egress_active;
    public long                                egress_time;
    public BACnetLightingInProgress            in_progress;
    public long                                lighting_command_default_priority;
    public String                              object_name;
    public boolean                             out_of_service;
    public BACnetPriorityArray                 priority_array;
    public BACnetPropertyIdentifierOrUnknown[] property_list;
    public float                               relinquish_default;
    public BACnetStatusFlags                   status_flags;
    public float                               tracking_value;

    //////////////////////////////
    //
    // RequiredAndWritable fields:
    //
    public BACnetLightingCommand               lighting_command;
    public float                               present_value;

    ///////////////////
    //
    // Optional fields:
    //
    public BACnetTimeStamp[]                   command_time_array;
    public float                               cov_increment;
    public String                              description;
    public float                               feedback_value;
    public float                               instantaneous_power;
    public BACnetTimeStamp                     last_command_time;
    public float                               max_actual_value;
    public float                               min_actual_value;
    public float                               power;
    public String                              profile_location;
    public String                              profile_name;
    public BACnetReliability                   reliability;
    public boolean                             reliability_evaluation_inhibit;
    public BACnetNameValue[]                   tags;
    public BACnetLightingTransition            transition;
    public BACnetValueSource                   value_source;
    public BACnetValueSource[]                 value_source_array;
    // @formatter:on

    public lighting_output()
    {
        super(BACnetObjectType.lighting_output);
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
