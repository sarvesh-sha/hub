/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.network;

import com.optio3.protocol.bacnet.ServiceContext;
import com.optio3.stream.InputBuffer;

public final class WhatIsNetworkNumberPDU extends NetworkMessagePDU
{
    public WhatIsNetworkNumberPDU(InputBuffer buffer)
    {
        decode(buffer);
    }

    @Override
    public void dispatch(ServiceContext sc)
    {
        sc.processNetworkMessageRequest(this);
    }
}
