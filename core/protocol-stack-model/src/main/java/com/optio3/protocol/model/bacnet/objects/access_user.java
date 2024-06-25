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
import com.optio3.protocol.model.bacnet.enums.BACnetAccessUserType;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectTypeOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetReliability;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetStatusFlags;

// NOTE: Generated automatically by regenerateObjectModels unit test!!
@JsonTypeName("BACnet:access_user")
public final class access_user extends BACnetObjectModel
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
    public List<BACnetDeviceObjectReference>   credentials;
    public String                              object_name;
    public BACnetPropertyIdentifierOrUnknown[] property_list;
    public BACnetReliability                   reliability;
    public BACnetStatusFlags                   status_flags;
    public BACnetAccessUserType                user_type;

    //////////////////////////////
    //
    // RequiredAndWritable fields:
    //
    public long                                global_identifier;

    ///////////////////
    //
    // Optional fields:
    //
    public String                              description;
    public List<BACnetDeviceObjectReference>   member_of;
    public List<BACnetDeviceObjectReference>   members;
    public String                              profile_location;
    public String                              profile_name;
    public boolean                             reliability_evaluation_inhibit;
    public BACnetNameValue[]                   tags;
    public String                              user_external_identifier;
    public String                              user_information_reference;
    public String                              user_name;
    // @formatter:on

    public access_user()
    {
        super(BACnetObjectType.access_user);
        credentials = Lists.newArrayList();
        member_of = Lists.newArrayList();
        members = Lists.newArrayList();
    }
}
