/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.BACnetObjectModel;
import com.optio3.protocol.model.bacnet.constructed.BACnetDateTime;
import com.optio3.protocol.model.bacnet.constructed.BACnetDeviceObjectPropertyReference;
import com.optio3.protocol.model.bacnet.constructed.BACnetNameValue;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectTypeOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;

// NOTE: Generated automatically by regenerateObjectModels unit test!!
@JsonTypeName("BACnet:averaging")
public final class averaging extends BACnetObjectModel
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
    public float                               average_value;
    public float                               maximum_value;
    public float                               minimum_value;
    public String                              object_name;
    public BACnetDeviceObjectPropertyReference object_property_reference;
    public BACnetPropertyIdentifierOrUnknown[] property_list;
    public long                                valid_samples;

    //////////////////////////////
    //
    // RequiredAndWritable fields:
    //
    public long                                attempted_samples;
    public long                                window_interval;
    public long                                window_samples;

    ///////////////////
    //
    // Optional fields:
    //
    public String                              description;
    public BACnetDateTime                      maximum_value_timestamp;
    public BACnetDateTime                      minimum_value_timestamp;
    public String                              profile_location;
    public String                              profile_name;
    public BACnetNameValue[]                   tags;
    public float                               variance_value;
    // @formatter:on

    public averaging()
    {
        super(BACnetObjectType.averaging);
    }
}
