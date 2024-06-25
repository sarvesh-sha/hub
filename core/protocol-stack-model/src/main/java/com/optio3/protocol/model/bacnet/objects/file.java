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
import com.optio3.protocol.model.bacnet.constructed.BACnetNameValue;
import com.optio3.protocol.model.bacnet.enums.BACnetFileAccessMethod;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectTypeOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;

// NOTE: Generated automatically by regenerateObjectModels unit test!!
@JsonTypeName("BACnet:file")
public final class file extends BACnetObjectModel
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
    public BACnetFileAccessMethod              file_access_method;
    public long                                file_size;
    public String                              file_type;
    public BACnetDateTime                      modification_date;
    public String                              object_name;
    public BACnetPropertyIdentifierOrUnknown[] property_list;
    public boolean                             read_only;

    //////////////////////////////
    //
    // RequiredAndWritable fields:
    //
    public boolean                             archive;

    ///////////////////
    //
    // Optional fields:
    //
    public String                              description;
    public String                              profile_location;
    public String                              profile_name;
    public long                                record_count;
    public BACnetNameValue[]                   tags;
    // @formatter:on

    public file()
    {
        super(BACnetObjectType.file);
    }
}
