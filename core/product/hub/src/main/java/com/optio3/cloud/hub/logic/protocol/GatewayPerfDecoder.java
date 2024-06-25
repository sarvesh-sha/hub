/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.protocol;

import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntity;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntitySelector;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.logic.spooler.StagedResultsSummary;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.logging.Logger;
import com.optio3.protocol.model.BaseAssetDescriptor;
import com.optio3.protocol.model.GatewayDescriptor;

public class GatewayPerfDecoder implements IProtocolDecoder
{
    @Override
    public GatewayDiscoveryEntitySelector getRootSelector()
    {
        return GatewayDiscoveryEntitySelector.Gateway;
    }

    @Override
    public String getProtocolValue()
    {
        return GatewayDiscoveryEntity.Protocol_Perf;
    }

    @Override
    public GatewayDiscoveryEntitySelector getDeviceSelector()
    {
        return GatewayDiscoveryEntitySelector.Perf_Device;
    }

    @Override
    public GatewayDiscoveryEntitySelector getDeviceReachabilitySelector()
    {
        return null;
    }

    @Override
    public GatewayDiscoveryEntitySelector getObjectSelector()
    {
        return GatewayDiscoveryEntitySelector.Perf_Object;
    }

    @Override
    public GatewayDiscoveryEntitySelector getSampleSelector()
    {
        return GatewayDiscoveryEntitySelector.Perf_ObjectSample;
    }

    @Override
    public GatewayDescriptor decodeDeviceContext(GatewayDiscoveryEntity en_root,
                                                 GatewayDiscoveryEntity en_device)
    {
        GatewayDescriptor desc = new GatewayDescriptor();
        desc.sysId = en_root.selectorValue;
        return desc;
    }

    @Override
    public void mergeDetails(BaseAssetDescriptor target,
                             BaseAssetDescriptor source)
    {
        // Nothing to do.
    }

    //--//

    @Override
    public void trackSampleIfNeeded(StagedResultsSummary.ForRoot root,
                                    BaseAssetDescriptor device,
                                    String objectIdentifier,
                                    double timestampEpochSeconds,
                                    String contents)
    {
        // Nothing to do.
    }

    @Override
    public void importDone(SessionHolder holder)
    {
        // Nothing to do.
    }

    //--//

    @Override
    public CommonProtocolHandlerForIngestion<?, ?> prepareIngestor(HubConfiguration cfg,
                                                                   Logger logger,
                                                                   StagedResultsSummary.ForAsset forAsset)
    {
        return new GatewayPerfHandlerForBatchIngestion(cfg, logger, forAsset);
    }
}
