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
import com.optio3.protocol.model.bacnet.BACnetDeviceDescriptor;

public class BACnetDecoder implements IProtocolDecoder
{
    @Override
    public GatewayDiscoveryEntitySelector getRootSelector()
    {
        return GatewayDiscoveryEntitySelector.Network;
    }

    @Override
    public String getProtocolValue()
    {
        return GatewayDiscoveryEntity.Protocol_BACnet;
    }

    @Override
    public GatewayDiscoveryEntitySelector getDeviceSelector()
    {
        return GatewayDiscoveryEntitySelector.BACnet_Device;
    }

    @Override
    public GatewayDiscoveryEntitySelector getDeviceReachabilitySelector()
    {
        return GatewayDiscoveryEntitySelector.BACnet_Reachability;
    }

    @Override
    public GatewayDiscoveryEntitySelector getObjectSelector()
    {
        return GatewayDiscoveryEntitySelector.BACnet_Object;
    }

    @Override
    public GatewayDiscoveryEntitySelector getSampleSelector()
    {
        return GatewayDiscoveryEntitySelector.BACnet_ObjectSample;
    }

    @Override
    public BACnetDeviceDescriptor decodeDeviceContext(GatewayDiscoveryEntity en_root,
                                                      GatewayDiscoveryEntity en_device)
    {
        return en_device.getSelectorValueAsObject(BACnetDeviceDescriptor.class);
    }

    @Override
    public void mergeDetails(BaseAssetDescriptor target,
                             BaseAssetDescriptor source)
    {
        BACnetDeviceDescriptor targetTyped = (BACnetDeviceDescriptor) target;
        BACnetDeviceDescriptor sourceTyped = (BACnetDeviceDescriptor) source;

        if (sourceTyped.bacnetAddress != null)
        {
            targetTyped.bacnetAddress = sourceTyped.bacnetAddress;
        }

        if (sourceTyped.transport != null)
        {
            targetTyped.transport = sourceTyped.transport;
        }

        if (sourceTyped.segmentation != null)
        {
            targetTyped.segmentation = sourceTyped.segmentation;
        }

        if (sourceTyped.maxAdpu > 0)
        {
            targetTyped.maxAdpu = sourceTyped.maxAdpu;
        }
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

    @Override
    public CommonProtocolHandlerForIngestion<?, ?> prepareIngestor(HubConfiguration cfg,
                                                                   Logger logger,
                                                                   StagedResultsSummary.ForAsset forAsset)
    {
        return new BACnetHandlerForBatchIngestion(cfg, logger, forAsset);
    }
}
