/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.simulator.remoting;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.annotation.Optio3RemotableEndpoint;
import com.optio3.cloud.client.gateway.model.GatewayAutoDiscovery;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntity;
import com.optio3.cloud.client.gateway.model.GatewayNetwork;
import com.optio3.cloud.client.gateway.model.GatewayOperationToken;
import com.optio3.cloud.client.gateway.orchestration.state.GatewayState;
import com.optio3.cloud.client.gateway.proxy.GatewayDiscoveryApi;

@Optio3RemotableEndpoint(itf = GatewayDiscoveryApi.class)
public class SimulatedGatewayDiscoveryApiImpl extends CommonSimulatedGatewayApiImpl implements GatewayDiscoveryApi
{
    @Override
    public CompletableFuture<GatewayOperationToken> performAutoDiscovery() throws
                                                                           Exception
    {
        return getTracker().trackOperation((operationContext) -> AsyncRuntime.True);
    }

    @Override
    public CompletableFuture<List<GatewayAutoDiscovery>> getAutoDiscoveryResults(GatewayOperationToken token) throws
                                                                                                              Exception
    {
        return CompletableFuture.completedFuture(Collections.emptyList());
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
        return getTracker().trackOperation((operationContext) -> getState().triggerDiscovery(operationContext, entities, broadcastIntervals, rebroadcastCount));
    }

    @Override
    public CompletableFuture<GatewayOperationToken> listObjects(List<GatewayDiscoveryEntity> entities) throws
                                                                                                       Exception
    {
        return getTracker().trackOperation((operationContext) -> getState().listObjects(operationContext, entities));
    }

    @Override
    public CompletableFuture<GatewayOperationToken> readAllValues(List<GatewayDiscoveryEntity> entities) throws
                                                                                                         Exception
    {
        return getTracker().trackOperation((operationContext) -> getState().readAllValues(operationContext, entities));
    }

    @Override
    public CompletableFuture<GatewayOperationToken> writeValues(List<GatewayDiscoveryEntity> entities) throws
                                                                                                       Exception
    {
        return getTracker().trackOperation((operationContext) -> getState().writeValues(operationContext, entities));
    }

    @Override
    public CompletableFuture<GatewayOperationToken> startSamplingConfiguration(List<GatewayDiscoveryEntity> entities) throws
                                                                                                                      Exception
    {
        return getTracker().trackOperation((operationContext) -> getState().startSamplingConfiguration(operationContext, entities));
    }

    @Override
    public CompletableFuture<GatewayOperationToken> updateSamplingConfiguration(List<GatewayDiscoveryEntity> entities) throws
                                                                                                                       Exception
    {
        return getTracker().trackOperation((operationContext) -> getState().updateSamplingConfiguration(operationContext, entities));
    }

    @Override
    public CompletableFuture<GatewayOperationToken> completeSamplingConfiguration(List<GatewayDiscoveryEntity> entities) throws
                                                                                                                         Exception
    {
        return getTracker().trackOperation((operationContext) -> getState().completeSamplingConfiguration(operationContext, entities));
    }

    @Override
    public CompletableFuture<GatewayOperationToken> flushEntities() throws
                                                                    Exception
    {
        return getTracker().trackOperation((operationContext) -> getState().flushEntities());
    }

    public GatewayState getState()
    {
        return getApplication().getState();
    }
}
