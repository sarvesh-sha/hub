/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.optio3.protocol.model.bacnet.BACnetDeviceDescriptor;
import com.optio3.protocol.model.ipn.IpnDeviceDescriptor;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes({ @JsonSubTypes.Type(value = BACnetDeviceDescriptor.class),
                @JsonSubTypes.Type(value = GatewayDescriptor.class),
                @JsonSubTypes.Type(value = GatewayPerfDescriptor.class),
                @JsonSubTypes.Type(value = IpnDeviceDescriptor.class),
                @JsonSubTypes.Type(value = NetworkDescriptor.class),
                @JsonSubTypes.Type(value = RestDescriptor.class),
                @JsonSubTypes.Type(value = RestPerfDescriptor.class),
                @JsonSubTypes.Type(value = TransportNetworkDescriptor.class) })
public abstract class BaseAssetDescriptor implements Comparable<BaseAssetDescriptor>
{
    public abstract String toString();
}
