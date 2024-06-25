/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.gateway.model;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.protocol.model.BaseObjectModel;
import com.optio3.protocol.model.config.ProtocolConfig;

public class GatewayAutoDiscovery
{
    public enum Flavor
    {
        ArgoHytos,
        Bergstrom,
        BlueSky,
        EpSolar,
        GPS,
        HendricksonWatchman,
        Holykell,
        J1939,
        MontageBluetoothGateway,
        MorningStar,
        OBDII,
        Palfinger,
        StealthPower,
        Victron
    }

    public Flavor                                 flavor;
    public ProtocolConfig                         cfg;
    public List<Class<? extends BaseObjectModel>> found = Lists.newArrayList();
}
