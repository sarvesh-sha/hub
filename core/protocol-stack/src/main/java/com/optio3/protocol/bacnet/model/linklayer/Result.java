/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.linklayer;

import com.optio3.protocol.bacnet.ServiceContext;
import com.optio3.protocol.model.bacnet.enums.BACnetVirtualLinkLayerResult;
import com.optio3.serialization.SerializationTag;

public final class Result extends BaseVirtualLinkLayer
{
    @SerializationTag(number = 0, width = 16)
    public BACnetVirtualLinkLayerResult result;

    @Override
    public void dispatch(ServiceContext sc)
    {
        sc.processLinkLayerRequest(this);
    }
}
