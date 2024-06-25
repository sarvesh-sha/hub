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
import com.optio3.protocol.model.bacnet.constructed.BACnetAccessRule;
import com.optio3.protocol.model.bacnet.constructed.BACnetDeviceObjectReference;
import com.optio3.protocol.model.bacnet.constructed.BACnetNameValue;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectTypeOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetReliability;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetStatusFlags;

// NOTE: Generated automatically by regenerateObjectModels unit test!!
@JsonTypeName("BACnet:access_rights")
public final class access_rights extends BACnetObjectModel
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
    public boolean                             enable;
    public BACnetAccessRule[]                  negative_access_rules;
    public String                              object_name;
    public BACnetAccessRule[]                  positive_access_rules;
    public BACnetPropertyIdentifierOrUnknown[] property_list;
    public BACnetReliability                   reliability;
    public BACnetStatusFlags                   status_flags;

    //////////////////////////////
    //
    // RequiredAndWritable fields:
    //
    public long                                global_identifier;

    ///////////////////
    //
    // Optional fields:
    //
    public BACnetDeviceObjectReference         accompaniment;
    public String                              description;
    public String                              profile_location;
    public String                              profile_name;
    public boolean                             reliability_evaluation_inhibit;
    public BACnetNameValue[]                   tags;
    // @formatter:on

    public access_rights()
    {
        super(BACnetObjectType.access_rights);
    }
}
