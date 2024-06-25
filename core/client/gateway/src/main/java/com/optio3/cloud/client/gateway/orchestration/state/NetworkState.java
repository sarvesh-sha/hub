/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.gateway.orchestration.state;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.util.concurrent.CompletableFuture;

import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntity;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntitySelector;
import com.optio3.cloud.client.gateway.model.GatewayNetwork;
import com.optio3.concurrency.AsyncMutex;

public abstract class NetworkState
{
    private final AsyncMutex m_mutex = new AsyncMutex();

    public final GatewayState   gatewayState;
    public final GatewayNetwork configuration;

    //--//

    protected NetworkState(GatewayState gatewayState,
                           GatewayNetwork configuration)
    {
        this.gatewayState  = gatewayState;
        this.configuration = configuration;
    }

    //--//

    public final CompletableFuture<Void> start() throws
                                                 Exception
    {
        try (AsyncMutex.Holder holder = await(m_mutex.acquire()))
        {
            await(startInner());
        }

        return wrapAsync(null);
    }

    protected abstract CompletableFuture<Void> startInner() throws
                                                            Exception;

    public final CompletableFuture<Void> stop() throws
                                                Exception
    {
        try (AsyncMutex.Holder holder = await(m_mutex.acquire()))
        {
            await(stopInner());
        }

        return wrapAsync(null);
    }

    protected abstract CompletableFuture<Void> stopInner() throws
                                                           Exception;

    //--//

    public CompletableFuture<Boolean> reload(GatewayState.PersistedNetworkConfiguration networkCfg) throws
                                                                                                    Exception
    {
        try (AsyncMutex.Holder holder = await(m_mutex.acquire()))
        {
            await(startInner());

            boolean result = await(reloadInner(networkCfg));
            return wrapAsync(result);
        }
    }

    protected abstract CompletableFuture<Boolean> reloadInner(GatewayState.PersistedNetworkConfiguration networkCfg) throws
                                                                                                                     Exception;
    //--//

    public abstract CompletableFuture<Boolean> discover(GatewayOperationTracker.State operationContext,
                                                        GatewayState.ResultHolder holder_network,
                                                        GatewayDiscoveryEntity en_network,
                                                        int broadcastIntervals,
                                                        int rebroadcastCount) throws
                                                                              Exception;

    //--//

    public abstract CompletableFuture<Boolean> listObjects(GatewayOperationTracker.State operationContext,
                                                           GatewayState.ResultHolder holder_network,
                                                           GatewayDiscoveryEntity en_network) throws
                                                                                              Exception;

    public abstract CompletableFuture<Boolean> readAllValues(GatewayOperationTracker.State operationContext,
                                                             GatewayState.ResultHolder holder_network,
                                                             GatewayDiscoveryEntity en_network) throws
                                                                                                Exception;

    public abstract CompletableFuture<Boolean> writeValues(GatewayOperationTracker.State operationContext,
                                                           GatewayState.ResultHolder holder_network,
                                                           GatewayDiscoveryEntity en_network) throws
                                                                                              Exception;
    //--//

    public abstract CompletableFuture<Boolean> startSamplingConfiguration(GatewayOperationTracker.State operationContext,
                                                                          GatewayState.ResultHolder holder_network,
                                                                          GatewayDiscoveryEntity en_network) throws
                                                                                                             Exception;

    public abstract CompletableFuture<Boolean> updateSamplingConfiguration(GatewayOperationTracker.State operationContext,
                                                                           GatewayState.ResultHolder holder_network,
                                                                           GatewayDiscoveryEntity en_network) throws
                                                                                                              Exception;

    public abstract CompletableFuture<Boolean> completeSamplingConfiguration(GatewayOperationTracker.State operationContext,
                                                                             GatewayState.ResultHolder holder_network,
                                                                             GatewayDiscoveryEntity en_network) throws
                                                                                                                Exception;

    //--//

    @FunctionalInterface
    protected static interface ProtocolEnumerationCallback
    {
        CompletableFuture<Boolean> process(GatewayState.ResultHolder holder_protocol,
                                           GatewayDiscoveryEntity en_protocol) throws
                                                                               Exception;
    }

    protected CompletableFuture<Boolean> enumerateProtocol(GatewayState.ResultHolder holder_network,
                                                           GatewayDiscoveryEntity en_network,
                                                           String protocolSelector,
                                                           ProtocolEnumerationCallback callback) throws
                                                                                                 Exception
    {
        boolean success = true;

        for (GatewayDiscoveryEntity en_protocol : en_network.filter(GatewayDiscoveryEntitySelector.Protocol, protocolSelector))
        {
            GatewayState.ResultHolder holder_protocol = holder_network.prepareResult(GatewayDiscoveryEntitySelector.Protocol, en_protocol.selectorValue, false);

            success &= await(callback.process(holder_protocol, en_protocol));
        }

        return wrapAsync(success);
    }
}