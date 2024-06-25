/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.linklayer;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.protocol.bacnet.ServiceContext;
import com.optio3.serialization.SerializationHelper;
import com.optio3.stream.InputBuffer;

public final class ReadBroadcastDistributionTable extends BaseVirtualLinkLayer
{
    public static final class Ack extends BaseVirtualLinkLayer
    {
        public List<BroadcastDistributionTableEntry> entries = Lists.newArrayList();

        @Override
        protected void decodePayload(InputBuffer payload)
        {
            while (!payload.isEOF())
            {
                BroadcastDistributionTableEntry bdt = new BroadcastDistributionTableEntry();

                SerializationHelper.read(payload, bdt);
                entries.add(bdt);
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
