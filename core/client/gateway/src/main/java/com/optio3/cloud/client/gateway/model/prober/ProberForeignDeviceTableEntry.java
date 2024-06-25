/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.gateway.model.prober;

import com.optio3.lang.Unsigned16;

public class ProberForeignDeviceTableEntry
{
    public String address;

    public Unsigned16 timeToLive;

    public Unsigned16 remainingTimeToLive;
}
