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
import com.optio3.protocol.model.bacnet.constructed.BACnetNameValue;
import com.optio3.protocol.model.bacnet.constructed.choice.BACnetCalendarEntry;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectTypeOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;

// NOTE: Generated automatically by regenerateObjectModels unit test!!
@JsonTypeName("BACnet:calendar")
public final class calendar extends BACnetObjectModel
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
    public List<BACnetCalendarEntry>           date_list;
    public String                              object_name;
    public boolean                             present_value;
    public BACnetPropertyIdentifierOrUnknown[] property_list;

    ///////////////////
    //
    // Optional fields:
    //
    public String                              description;
    public String                              profile_location;
    public String                              profile_name;
    public BACnetNameValue[]                   tags;
    // @formatter:on

    public calendar()
    {
        super(BACnetObjectType.calendar);
        date_list = Lists.newArrayList();
    }
}
