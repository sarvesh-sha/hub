/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.linklayer;

import com.optio3.lang.Unsigned16;
import com.optio3.lang.Unsigned32;
import com.optio3.protocol.bacnet.ServiceContext;
import com.optio3.serialization.SerializationTag;

public final class DeleteForeignDeviceTableEntry extends BaseVirtualLinkLayer
{
    @SerializationTag(number = 0)
    public Unsigned16 network_number;

    @SerializationTag(number = 1)
    public Unsigned32 device_address;

    //--//

    @Override
    public void dispatch(ServiceContext sc)
    {
        sc.processLinkLayerRequest(this);
    }
}
