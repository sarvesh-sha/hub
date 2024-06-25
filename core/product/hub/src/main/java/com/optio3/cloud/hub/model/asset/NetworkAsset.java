/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.asset;

import java.time.ZonedDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.DiscoveryState;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.protocol.model.config.ProtocolConfig;

@JsonTypeName("NetworkAsset")
public class NetworkAsset extends Asset
{
    public String cidr;

    public String staticAddress;

    public String networkInterface;

    public int samplingPeriod;

    public List<ProtocolConfig> protocolsConfiguration = Lists.newArrayList();

    @Optio3MapAsReadOnly
    public DiscoveryState discoveryState;

    //--//

    @Optio3MapAsReadOnly
    public ZonedDateTime lastOutput;

    @Optio3MapAsReadOnly
    public int lastOffset;

    //--//

    @Override
    public AssetRecord newRecord()
    {
        return new NetworkAssetRecord();
    }
}
