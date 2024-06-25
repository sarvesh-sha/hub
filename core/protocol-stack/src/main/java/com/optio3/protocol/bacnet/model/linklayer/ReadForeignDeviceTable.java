/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.linklayer;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.protocol.bacnet.ServiceContext;
import com.optio3.protocol.model.bacnet.serialization.BACnetSerializationTag;
import com.optio3.serialization.SerializationHelper;
import com.optio3.serialization.SerializationTag;
import com.optio3.stream.InputBuffer;

public final class ReadForeignDeviceTable extends BaseVirtualLinkLayer
{
    public static final class Ack extends BaseVirtualLinkLayer
    {
        @SerializationTag(number = 0)
        @BACnetSerializationTag(untagged = true)
        public List<ForeignDeviceTableEntry> entries = Lists.newArrayList();

        @Override
        protected void decodePayload(InputBuffer payload)
        {
            while (!payload.isEOF())
            {
                ForeignDeviceTableEntry fdt = new ForeignDeviceTableEntry();

                SerializationHelper.read(payload, fdt);
                entries.add(fdt);
            }
        }

        @Override
        public void dispatch(ServiceContext sc)
        {
            sc.processLinkLayerRequest(this);
        }
    }

    //--//

    @Override
    public void dispatch(ServiceContext sc)
    {
        sc.processLinkLayerRequest(this);
    }
}
