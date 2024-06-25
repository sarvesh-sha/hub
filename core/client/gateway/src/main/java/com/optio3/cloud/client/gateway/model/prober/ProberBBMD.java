/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.gateway.model.prober;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.protocol.model.config.BACnetBBMD;

public class ProberBBMD
{
    public BACnetBBMD descriptor;

    public List<ProberBroadcastDistributionTableEntry> bdt = Lists.newArrayList();

    public List<ProberForeignDeviceTableEntry> fdt = Lists.newArrayList();
}
