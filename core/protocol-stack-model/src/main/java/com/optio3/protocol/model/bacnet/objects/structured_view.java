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
import com.optio3.protocol.model.bacnet.constructed.BACnetDeviceObjectReference;
import com.optio3.protocol.model.bacnet.constructed.BACnetNameValue;
import com.optio3.protocol.model.bacnet.constructed.BACnetNameValueCollection;
import com.optio3.protocol.model.bacnet.enums.BACnetNodeType;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectTypeOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetRelationship;

// NOTE: Generated automatically by regenerateObjectModels unit test!!
@JsonTypeName("BACnet:structured_view")
public final class structured_view extends BACnetObjectModel
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
    public BACnetNodeType                      node_type;
    public String                              object_name;
    public BACnetPropertyIdentifierOrUnknown[] property_list;
    public BACnetDeviceObjectReference[]       subordinate_list;

    ///////////////////
    //
    // Optional fields:
    //
    public BACnetRelationship                  default_subordinate_relationship;
    public String                              description;
    public String                              node_subtype;
    public String                              profile_location;
    public String                              profile_name;
    public BACnetDeviceObjectReference         represents;
    public String[]                            subordinate_annotations;
    public BACnetNodeType[]                    subordinate_node_types;
    public BACnetRelationship[]                subordinate_relationships;
    public BACnetNameValueCollection[]         subordinate_tags;
    public BACnetNameValue[]                   tags;
    // @formatter:on

    public structured_view()
    {
        super(BACnetObjectType.structured_view);
    }
}
