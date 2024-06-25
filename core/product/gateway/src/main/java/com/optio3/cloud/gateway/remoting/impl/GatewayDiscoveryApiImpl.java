/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.gateway.remoting.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import com.optio3.cloud.annotation.Optio3RemotableEndpoint;
import com.optio3.cloud.client.gateway.model.GatewayAutoDiscovery;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntity;
import com.optio3.cloud.client.gateway.model.GatewayNetwork;
import com.optio3.cloud.client.gateway.model.GatewayOperationToken;
import com.optio3.cloud.client.gateway.orchestration.state.GatewayState;
import com.optio3.cloud.client.gateway.proxy.GatewayDiscoveryApi;
import com.optio3.cloud.gateway.GatewayApplication;

@Optio3RemotableEndpoint(itf = GatewayDiscoveryApi.class)
public class GatewayDiscoveryApiImpl extends CommonGatewayApiImpl implements GatewayDiscoveryApi
{
    @Inject
    private GatewayApplication m_app;

    //--//

    @Override
    public CompletableFuture<GatewayOperationToken> performAutoDiscovery() throws
                                                                           Exception
    {
        return getTracker().trackOperation((operationHolder) -> getState().performAutoDiscovery(operationHolder));
    }

    @Override
    public CompletableFuture<List<GatewayAutoDiscovery>> getAutoDiscoveryResults(GatewayOperationToken token)
    {
        return getState().getAutoDiscoveryResults(m_app.getOperationTracker(), token);
    }

    //--//

    @Override
    public CompletableFuture<List<String>> assignNetworks(List<GatewayNetwork> networks)
    {
        return getState().importNetworks(networks);
    }

    @Override
    public CompletableFuture<GatewayOperationToken> triggerDiscovery(List<GatewayDiscoveryEntity> entities,
                                                                     int broadcastIntervals,
                                                                     int rebroadcastCount) throws
                                                                                           Exception
    {
        return getTracker().trackOperation((operationHolder) -> getState().triggerDiscovery(operationHolder, entities, broadcastIntervals, rebroadcastCount));
    }

    @Override
    public CompletableFuture<GatewayOperationToken> listObjects(List<GatewayDiscoveryEntity> entities) throws
                                                                                                       Exception
    {
        return getTracker().trackOperation((operationHolder) -> getState().listObjects(operationHolder, entities));
    }

    @Override
    public CompletableFuture<GatewayOperationToken> readAllValues(List<GatewayDiscoveryEntity> entities) throws
                                                                                                         Exception
    {
        return getTracker().trackOperation((operationHolder) -> getState().readAllValues(operationHolder, entities));
    }

    @Override
    public CompletableFuture<GatewayOperationToken> writeValues(List<GatewayDiscoveryEntity> entities) throws
                                                                                                       Exception
    {
        return getTracker().trackOperation((operationHolder) -> getState().writeValues(operationHolder, entities));
    }

    @Override
    public CompletableFuture<GatewayOperationToken> startSamplingConfiguration(List<GatewayDiscoveryEntity> entities) throws
                                                                                                                      Exception
    {
        return getTracker().trackOperation((operationHolder) -> getState().startSamplingConfiguration(operationHolder, entities));
    }

    @Override
    public CompletableFuture<GatewayOperationToken> updateSamplingConfiguration(List<GatewayDiscoveryEntity> entities) throws
                                                                                                                       Exception
    {
        return getTracker().trackOperation((operationHolder) -> getState().updateSamplingConfiguration(operationHolder, entities));
    }

    @Override
    public CompletableFuture<GatewayOperationToken> completeSamplingConfiguration(List<GatewayDiscoveryEntity> entities) throws
                                                                                                                         Exception
    {
        return getTracker().trackOperation((operationHolder) -> getState().completeSamplingConfiguration(operationHolder, entities));
    }

    @Override
    public CompletableFuture<GatewayOperationToken> flushEntities() throws
                                                                    Exception
    {
        return getTracker().trackOperation((operationHolder) -> getState().flushEntities());
    }

    //--//

    private GatewayState getState()
    {
        return m_app.getServiceNonNull(GatewayState.class);
    }
}
