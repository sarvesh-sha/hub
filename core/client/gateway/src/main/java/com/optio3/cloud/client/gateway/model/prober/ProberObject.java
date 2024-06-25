/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.gateway.model.prober;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.optio3.protocol.model.BaseAssetDescriptor;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes({ @JsonSubTypes.Type(value = ProberObjectBACnet.class), @JsonSubTypes.Type(value = ProberObjectCANbus.class), @JsonSubTypes.Type(value = ProberObjectIpn.class) })
public abstract class ProberObject implements Comparable<ProberObject>
{
    public BaseAssetDescriptor device;

    public String objectId;

    public JsonNode properties;

    //--//

    @Override
    public int compareTo(ProberObject o)
    {
        int diff = device.compareTo(o.device);
        if (diff == 0)
        {
            diff = compareObjectId(o);
        }

        return diff;
    }

    protected abstract int compareObjectId(ProberObject o);
}