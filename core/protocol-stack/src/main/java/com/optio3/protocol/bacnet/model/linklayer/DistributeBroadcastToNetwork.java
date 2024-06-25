/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.linklayer;

import com.optio3.protocol.bacnet.ServiceContext;

public final class DistributeBroadcastToNetwork extends NetworkPayload
{
    @Override
    public void dispatch(ServiceContext sc)
    {
        dispatchInner(sc);
    }
}
