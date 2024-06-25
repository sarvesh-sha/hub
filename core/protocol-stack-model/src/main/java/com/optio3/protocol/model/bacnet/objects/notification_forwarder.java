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
import com.optio3.protocol.model.bacnet.constructed.BACnetDestination;
import com.optio3.protocol.model.bacnet.constructed.BACnetEventNotificationSubscription;
import com.optio3.protocol.model.bacnet.constructed.BACnetNameValue;
import com.optio3.protocol.model.bacnet.constructed.BACnetPortPermission;
import com.optio3.protocol.model.bacnet.constructed.choice.BACnetProcessIdSelection;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectTypeOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetReliability;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetStatusFlags;

// NOTE: Generated automatically by regenerateObjectModels unit test!!
@JsonTypeName("BACnet:notification_forwarder")
public final class notification_forwarder extends BACnetObjectModel
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
    public boolean                                   local_forwarding_only;
    public String                                    object_name;
    public boolean                                   out_of_service;
    public BACnetProcessIdSelection                  process_identifier_filter;
    public BACnetPropertyIdentifierOrUnknown[]       property_list;
    public List<BACnetDestination>                   recipient_list;
    public BACnetReliability                         reliability;
    public BACnetStatusFlags                         status_flags;

    //////////////////////////////
    //
    // RequiredAndWritable fields:
    //
    public List<BACnetEventNotificationSubscription> subscribed_recipients;

    ///////////////////
    //
    // Optional fields:
    //
    public String                                    description;
    public BACnetPortPermission[]                    port_filter;
    public String                                    profile_location;
    public String                                    profile_name;
    public boolean                                   reliability_evaluation_inhibit;
    public BACnetNameValue[]                         tags;
    // @formatter:on

    public notification_forwarder()
    {
        super(BACnetObjectType.notification_forwarder);
        recipient_list = Lists.newArrayList();
        subscribed_recipients = Lists.newArrayList();
    }
}
