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
import com.optio3.protocol.model.bacnet.constructed.BACnetAssignedAccessRights;
import com.optio3.protocol.model.bacnet.constructed.BACnetCredentialAuthenticationFactor;
import com.optio3.protocol.model.bacnet.constructed.BACnetDateTime;
import com.optio3.protocol.model.bacnet.constructed.BACnetDeviceObjectReference;
import com.optio3.protocol.model.bacnet.constructed.BACnetNameValue;
import com.optio3.protocol.model.bacnet.enums.BACnetAccessCredentialDisable;
import com.optio3.protocol.model.bacnet.enums.BACnetAccessCredentialDisableReason;
import com.optio3.protocol.model.bacnet.enums.BACnetAccessEvent;
import com.optio3.protocol.model.bacnet.enums.BACnetAuthorizationExemption;
import com.optio3.protocol.model.bacnet.enums.BACnetBinaryPV;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectTypeOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetReliability;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetStatusFlags;

// NOTE: Generated automatically by regenerateObjectModels unit test!!
@JsonTypeName("BACnet:access_credential")
public final class access_credential extends BACnetObjectModel
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
    public BACnetDateTime                            activation_time;
    public BACnetAssignedAccessRights[]              assigned_access_rights;
    public BACnetCredentialAuthenticationFactor[]    authentication_factors;
    public BACnetAccessCredentialDisable             credential_disable;
    public BACnetBinaryPV                            credential_status;
    public BACnetDateTime                            expiration_time;
    public String                                    object_name;
    public BACnetPropertyIdentifierOrUnknown[]       property_list;
    public List<BACnetAccessCredentialDisableReason> reason_for_disable;
    public BACnetReliability                         reliability;
    public BACnetStatusFlags                         status_flags;

    //////////////////////////////
    //
    // RequiredAndWritable fields:
    //
    public long                                      global_identifier;

    ///////////////////
    //
    // Optional fields:
    //
    public long                                      absentee_limit;
    public List<BACnetAuthorizationExemption>        authorization_exemptions;
    public BACnetDeviceObjectReference               belongs_to;
    public int                                       days_remaining;
    public String                                    description;
    public boolean                                   extended_time_enable;
    public BACnetAccessEvent                         last_access_event;
    public BACnetDeviceObjectReference               last_access_point;
    public BACnetDateTime                            last_use_time;
    public String                                    profile_location;
    public String                                    profile_name;
    public boolean                                   reliability_evaluation_inhibit;
    public BACnetNameValue[]                         tags;
    public byte                                      threat_authority;
    public boolean                                   trace_flag;
    public int                                       uses_remaining;
    // @formatter:on

    public access_credential()
    {
        super(BACnetObjectType.access_credential);
        authorization_exemptions = Lists.newArrayList();
        reason_for_disable = Lists.newArrayList();
    }
}
