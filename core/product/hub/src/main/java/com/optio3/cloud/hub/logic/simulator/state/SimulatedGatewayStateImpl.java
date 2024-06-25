/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.simulator.state;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.optio3.cloud.JsonWebSocket;
import com.optio3.cloud.client.gateway.model.GatewayNetwork;
import com.optio3.cloud.client.gateway.orchestration.state.GatewayOperationTracker;
import com.optio3.cloud.client.gateway.orchestration.state.GatewayState;
import com.optio3.cloud.client.gateway.orchestration.state.NetworkState;
import com.optio3.cloud.hub.logic.simulator.SimulatedGateway;
import com.optio3.cloud.messagebus.WellKnownDestination;
import com.optio3.cloud.messagebus.channel.RpcClient;
import com.optio3.util.ConfigurationPersistenceHelper;

public class SimulatedGatewayStateImpl extends GatewayState
{
    private SimulatedGateway m_app;

    public SimulatedGatewayStateImpl(SimulatedGateway app)
    {
        // Keep low serialization threshold to test the feature.
        super(new ConfigurationPersistenceHelper((String) null, null), 1, 3600, 1024, 1024, 1 * 1024 * 1024);

        m_app = app;
    }

    @Override
    public CompletableFuture<Boolean> performAutoDiscovery(GatewayOperationTracker.State operationContext)
    {
        return wrapAsync(true);
    }

    @Override
    protected NetworkState allocateNetworkState(GatewayNetwork network)
    {
        return new SimulatedNetworkStateImpl(m_app, network);
    }

    @Override
    protected <T> CompletableFuture<T> getProxy(Class<T> clz,
                                                int timeout,
                                                TimeUnit timeoutUnit) throws
                                                                      Exception
    {
        RpcClient client = await(m_app.getRpcClient(), 10, TimeUnit.SECONDS);
        T         proxy  = client.createProxy(WellKnownDestination.Service.getId(), null, clz, timeout, timeoutUnit);

        return wrapAsync(proxy);
    }

    @Override
    protected ObjectMapper getObjectMapper()
    {
        return JsonWebSocket.getObjectMapper();
    }

    @Override
    protected boolean isCellularConnection()
    {
        return false;
    }

    @Override
    public void reportSamplingDone(int sequenceNumber,
                                   String suffix,
                                   long samplingSlot,
                                   int period,
                                   ProgressStatus stats)
    {
        // Nothing to do.
    }
}
