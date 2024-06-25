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
import com.optio3.lang.Unsigned8;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.BACnetObjectModel;
import com.optio3.protocol.model.bacnet.constructed.BACnetAddressBinding;
import com.optio3.protocol.model.bacnet.constructed.BACnetNameValue;
import com.optio3.protocol.model.bacnet.constructed.BACnetNetworkSecurityPolicy;
import com.optio3.protocol.model.bacnet.constructed.BACnetSecurityKeySet;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectTypeOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetSecurityLevel;

// NOTE: Generated automatically by regenerateObjectModels unit test!!
@JsonTypeName("BACnet:network_security")
public final class network_security extends BACnetObjectModel
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
    public long                                distribution_key_revision;
    public BACnetSecurityKeySet[]              key_sets;
    public String                              object_name;
    public BACnetPropertyIdentifierOrUnknown[] property_list;
    public List<Unsigned8>                     supported_security_algorithms;
    public long                                update_key_set_timeout;

    //////////////////////////////
    //
    // RequiredAndWritable fields:
    //
    public BACnetSecurityLevel                 base_device_security_policy;
    public boolean                             do_not_hide;
    public BACnetAddressBinding                last_key_server;
    public BACnetNetworkSecurityPolicy[]       network_access_security_policies;
    public long                                packet_reorder_time;
    public long                                security_pdu_timeout;
    public long                                security_time_window;

    ///////////////////
    //
    // Optional fields:
    //
    public String                              description;
    public String                              profile_location;
    public String                              profile_name;
    public BACnetNameValue[]                   tags;
    // @formatter:on

    public network_security()
    {
        super(BACnetObjectType.network_security);
        key_sets = new BACnetSecurityKeySet[2];
        key_sets[0] = new BACnetSecurityKeySet();
        key_sets[1] = new BACnetSecurityKeySet();
        supported_security_algorithms = Lists.newArrayList();
    }
}
