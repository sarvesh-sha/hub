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

public interface IProtocolDecoder
{
    GatewayDiscoveryEntitySelector getRootSelector();

    String getProtocolValue();

    GatewayDiscoveryEntitySelector getDeviceSelector();

    GatewayDiscoveryEntitySelector getDeviceReachabilitySelector();

    GatewayDiscoveryEntitySelector getObjectSelector();

    GatewayDiscoveryEntitySelector getSampleSelector();

    BaseAssetDescriptor decodeDeviceContext(GatewayDiscoveryEntity en_root,
                                            GatewayDiscoveryEntity en_device);

    void mergeDetails(BaseAssetDescriptor target,
                      BaseAssetDescriptor source);

    void trackSampleIfNeeded(StagedResultsSummary.ForRoot root,
                             BaseAssetDescriptor device,
                             String objectIdentifier,
                             double timestampEpochSeconds,
                             String contents);

    void importDone(SessionHolder holder);

    CommonProtocolHandlerForIngestion<?, ?> prepareIngestor(HubConfiguration cfg,
                                                            Logger logger,
                                                            StagedResultsSummary.ForAsset forAsset);
}
