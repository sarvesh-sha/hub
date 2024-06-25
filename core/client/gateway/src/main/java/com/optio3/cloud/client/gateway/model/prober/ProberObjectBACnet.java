/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.gateway.model.prober;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;

@JsonTypeName("ProberObjectBACnet")
public class ProberObjectBACnet extends ProberObject
{
    @Override
    protected int compareObjectId(ProberObject o)
    {
        BACnetObjectIdentifier a = new BACnetObjectIdentifier(objectId);
        BACnetObjectIdentifier b = new BACnetObjectIdentifier(o.objectId);

        return BACnetObjectIdentifier.compare(a, b);
    }
}