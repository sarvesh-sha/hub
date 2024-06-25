/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.simulator.state;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.util.concurrent.CompletableFuture;

import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntity;
import com.optio3.cloud.client.gateway.model.GatewayNetwork;
import com.optio3.cloud.client.gateway.orchestration.state.GatewayOperationTracker;
import com.optio3.cloud.client.gateway.orchestration.state.GatewayState;
import com.optio3.cloud.client.gateway.orchestration.state.NetworkState;
import com.optio3.cloud.hub.logic.simulator.SimulatedGateway;

public class SimulatedNetworkStateImpl extends NetworkState
{
    private SimulatedBACnetState m_bacnet;
    private SimulatedGateway     m_gateway;

    protected SimulatedNetworkStateImpl(SimulatedGateway gateway,
                                        GatewayNetwork configuration)
    {
        super(gateway.getState(), configuration);
        m_gateway = gateway;
    }

    @Override
    protected CompletableFuture<Void> startInner() throws
                                                   Exception
    {
        SimulatedBACnetState bacnet = new SimulatedBACnetState(m_gateway, configuration);
        if (await(bacnet.start()))
        {
            m_bacnet = bacnet;
        }

        return wrapAsync(null);
    }

    @Override
    protected CompletableFuture<Void> stopInner() throws
                                                  Exception
    {
        if (m_bacnet != null)
        {
            await(m_bacnet.stop());
            m_bacnet = null;
        }

        return wrapAsync(null);
    }

    @Override
    protected CompletableFuture<Boolean> reloadInner(GatewayState.PersistedNetworkConfiguration networkCfg)
    {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public CompletableFuture<Boolean> discover(GatewayOperationTracker.State operationContext,
                                               GatewayState.ResultHolder holder_network,
                                               GatewayDiscoveryEntity en_network,
                                               int broadcastIntervals,
                                               int rebroadcastCount) throws
                                                                     Exception
    {
        boolean success = true;

        success &= await(enumerateProtocol(holder_network, en_network, GatewayDiscoveryEntity.Protocol_BACnet, (holder_protocol, en_protocol) ->
        {
            boolean successSub = true;

            if (m_bacnet != null)
            {
                successSub &= await(m_bacnet.discover(operationContext, holder_protocol, broadcastIntervals, rebroadcastCount));
            }

            return wrapAsync(successSub);
        }));

        return wrapAsync(success);
    }

    @Override
    public CompletableFuture<Boolean> listObjects(GatewayOperationTracker.State operationContext,
                                                  GatewayState.ResultHolder holder_network,
                                                  GatewayDiscoveryEntity en_network) throws
                                                                                     Exception
    {
        boolean success = true;

        success &= await(enumerateProtocol(holder_network, en_network, GatewayDiscoveryEntity.Protocol_BACnet, (holder_protocol, en_protocol) ->
        {
            if (m_bacnet != null)
            {
                await(m_bacnet.listObjects(operationContext, holder_protocol, en_protocol));
            }

            return wrapAsync(true);
        }));

        return wrapAsync(success);
    }

    @Override
    public CompletableFuture<Boolean> readAllValues(GatewayOperationTracker.State operationContext,
                                                    GatewayState.ResultHolder holder_network,
                                                    GatewayDiscoveryEntity en_network) throws
                                                                                       Exception
    {
        boolean success = true;

        success &= await(enumerateProtocol(holder_network, en_network, GatewayDiscoveryEntity.Protocol_BACnet, (holder_protocol, en_protocol) ->
        {
            if (m_bacnet != null)
            {
                await(m_bacnet.readAllValues(operationContext, holder_protocol, en_protocol));
            }

            return wrapAsync(true);
        }));

        return wrapAsync(success);
    }

    @Override
    public CompletableFuture<Boolean> writeValues(GatewayOperationTracker.State operationContext,
                                                  GatewayState.ResultHolder holder_network,
                                                  GatewayDiscoveryEntity en_network) throws
                                                                                     Exception
    {
        return wrapAsync(true); // Not implemented
    }

    @Override
    public CompletableFuture<Boolean> startSamplingConfiguration(GatewayOperationTracker.State operationContext,
                                                                 GatewayState.ResultHolder holder_network,
                                                                 GatewayDiscoveryEntity en_network) throws
                                                                                                    Exception
    {
        boolean success = true;

        success &= await(enumerateProtocol(holder_network, en_network, GatewayDiscoveryEntity.Protocol_BACnet, (holder_protocol, en_protocol) ->
        {
            if (m_bacnet != null)
            {
                await(m_bacnet.startSamplingConfiguration(operationContext));
            }

            return wrapAsync(true);
        }));

        return wrapAsync(success);
    }

    @Override
    public CompletableFuture<Boolean> updateSamplingConfiguration(GatewayOperationTracker.State operationContext,
                                                                  GatewayState.ResultHolder holder_network,
                                                                  GatewayDiscoveryEntity en_network) throws
                                                                                                     Exception
    {
        boolean success = true;

        success &= await(enumerateProtocol(holder_network, en_network, GatewayDiscoveryEntity.Protocol_BACnet, (holder_protocol, en_protocol) ->
        {
            if (m_bacnet != null)
            {
                await(m_bacnet.updateSamplingConfiguration(operationContext, en_protocol));
            }

            return wrapAsync(true);
        }));

        return wrapAsync(success);
    }

    @Override
    public CompletableFuture<Boolean> completeSamplingConfiguration(GatewayOperationTracker.State operationContext,
                                                                    GatewayState.ResultHolder holder_network,
                                                                    GatewayDiscoveryEntity en_network) throws
                                                                                                       Exception
    {
        boolean success = true;

        success &= await(enumerateProtocol(holder_network, en_network, GatewayDiscoveryEntity.Protocol_BACnet, (holder_protocol, en_protocol) ->
        {
            if (m_bacnet != null)
            {
                await(m_bacnet.completeSamplingConfiguration(operationContext, en_protocol.contents));
            }

            return wrapAsync(true);
        }));

        return wrapAsync(success);
    }
}
