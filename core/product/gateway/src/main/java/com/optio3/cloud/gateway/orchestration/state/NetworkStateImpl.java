/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.gateway.orchestration.state;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntity;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntitySelector;
import com.optio3.cloud.client.gateway.model.GatewayNetwork;
import com.optio3.cloud.client.gateway.orchestration.state.GatewayOperationTracker;
import com.optio3.cloud.client.gateway.orchestration.state.GatewayState;
import com.optio3.cloud.client.gateway.orchestration.state.NetworkState;
import com.optio3.concurrency.Executors;
import com.optio3.infra.NetworkHelper;
import com.optio3.protocol.model.BaseAssetDescriptor;
import com.optio3.protocol.model.NetworkDescriptor;
import com.optio3.protocol.model.TransportPerformanceCounters;
import com.optio3.protocol.model.config.ProtocolConfig;
import com.optio3.protocol.model.config.ProtocolConfigForBACnet;
import com.optio3.protocol.model.config.ProtocolConfigForIpn;
import com.optio3.util.ResourceAutoCleaner;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

public class NetworkStateImpl extends NetworkState
{
    private static final int SAMPLING_PERIOD = 5 * 60;

    //--//

    class ProtocolRegistration
    {
        final String                                                 selector;
        final Class<? extends ProtocolConfig>                        clzConfig;
        final BiFunction<GatewayState, GatewayNetwork, CommonState>  allocator;
        final Map<BaseAssetDescriptor, TransportPerformanceCounters> statistics = Maps.newHashMap();

        Object                           stateKeepAlive;
        ResourceAutoCleaner<CommonState> state;

        public ProtocolRegistration(String selector,
                                    Class<? extends ProtocolConfig> clzConfig,
                                    BiFunction<GatewayState, GatewayNetwork, CommonState> allocator)
        {
            this.selector  = selector;
            this.clzConfig = clzConfig;
            this.allocator = allocator;
        }

        void sampleStatistics(long samplingSlot,
                              BaseAssetDescriptor desc,
                              TransportPerformanceCounters stats) throws
                                                                  IOException
        {
            if (stats.hasNoValues())
            {
                return;
            }

            final TransportPerformanceCounters statsBefore = statistics.get(desc);
            final TransportPerformanceCounters delta       = statsBefore != null ? TransportPerformanceCounters.difference(statsBefore, stats) : null;
            final boolean                      firstPass   = delta == null;

            if (delta != null && delta.hasNoValues())
            {
                return;
            }

            stats = stats.copy();

            GatewayState.ResultHolder holder_root     = gatewayState.getRoot(samplingSlot);
            GatewayState.ResultHolder holder_network  = holder_root.prepareResult(GatewayDiscoveryEntitySelector.Network, configuration.sysId, false);
            GatewayState.ResultHolder holder_protocol = holder_network.prepareResult(GatewayDiscoveryEntitySelector.Protocol, GatewayDiscoveryEntity.Protocol_Perf, false);

            NetworkDescriptor deviceDesc = new NetworkDescriptor();
            deviceDesc.sysId = configuration.sysId;

            GatewayState.ResultHolder holder_device = holder_protocol.prepareResult(GatewayDiscoveryEntitySelector.Perf_Device, deviceDesc, firstPass);
            GatewayState.ResultHolder holder_object = holder_device.prepareResult(GatewayDiscoveryEntitySelector.Perf_Object, desc.toString(), firstPass);

            if (firstPass)
            {
                //
                // If this is the first time we see this counter,
                // queue some fake contents for the Hub.
                // That way the record for the counter will be created.
                //
                holder_device.queueContents("<trigger>");
                holder_object.queueContents("<trigger>");
            }
            else
            {
                GatewayState.ResultHolder holder_sample = holder_object.prepareResult(GatewayDiscoveryEntitySelector.Perf_ObjectSample, (String) null, true);
                holder_sample.queueContents(delta.serializeToJson());
            }

            statistics.put(desc, stats);
        }
    }

    private final List<ProtocolRegistration> m_protocols = Lists.newArrayList();
    private       int                        m_samplingToken;

    //--//

    public NetworkStateImpl(GatewayStateImpl gatewayState,
                            GatewayNetwork configuration)
    {
        super(gatewayState, configuration);

        m_protocols.add(new ProtocolRegistration(GatewayDiscoveryEntity.Protocol_BACnet, ProtocolConfigForBACnet.class, BACnetState::new));

        m_protocols.add(new ProtocolRegistration(GatewayDiscoveryEntity.Protocol_Ipn, ProtocolConfigForIpn.class, IpnState::new));
    }

    //--//

    @Override
    protected CompletableFuture<Void> startInner() throws
                                                   Exception
    {
        boolean hasInterface     = StringUtils.isNotEmpty(configuration.networkInterface);
        boolean hasStaticAddress = StringUtils.isNotEmpty(configuration.staticAddress);

        if (hasInterface && hasStaticAddress)
        {
            InetAddress                         ourAddress  = InetAddress.getByName(configuration.staticAddress);
            NetworkHelper.InetAddressWithPrefix networkInfo = NetworkHelper.InetAddressWithPrefix.parse(configuration.cidr);

            networkInfo = new NetworkHelper.InetAddressWithPrefix(ourAddress, networkInfo.prefixLength);

            NetworkHelper.setNetworks(configuration.networkInterface, networkInfo);
        }

        for (ProtocolRegistration pr : m_protocols)
        {
            if (configuration.hasProtocolConfiguration(pr.clzConfig))
            {
                pr.stateKeepAlive = new Object();
                ResourceAutoCleaner<CommonState> state = new ResourceAutoCleaner<>(pr.stateKeepAlive, pr.allocator.apply(gatewayState, configuration));
                if (await(state.resource.start()))
                {
                    pr.state = state;

                    queueNextPerfCounterSampling(++m_samplingToken);
                }
            }
        }

        return wrapAsync(null);
    }

    @Override
    protected CompletableFuture<Void> stopInner() throws
                                                  Exception
    {
        for (ProtocolRegistration pr : m_protocols)
        {
            ResourceAutoCleaner<CommonState> state = pr.state;
            pr.stateKeepAlive = null;
            pr.state          = null;

            if (state != null)
            {
                await(state.resource.stop());
            }

            pr.statistics.clear();
        }

        return wrapAsync(null);
    }

    private void queueNextPerfCounterSampling(int samplingToken)
    {
        if (samplingToken == m_samplingToken)
        {
            long now = TimeUtils.nowEpochSeconds();

            //
            // Align sampling time to a multiple of the sampling period.
            // That way we always sample at the same time, every day.
            //
            long nextSample = TimeUtils.adjustGranularity(now + SAMPLING_PERIOD, SAMPLING_PERIOD);

            long nowMilliUtc      = TimeUtils.nowMilliUtc();
            long diffMilliseconds = Math.max(0, (nextSample * 1_000 - nowMilliUtc));

            Executors.scheduleOnDefaultPool(() -> executePerfCounterSampling(samplingToken, nextSample), diffMilliseconds, TimeUnit.MILLISECONDS);
        }
    }

    private void executePerfCounterSampling(int samplingToken,
                                            long samplingSlot)
    {
        for (ProtocolRegistration pr : m_protocols)
        {
            if (pr.state != null)
            {
                try
                {
                    pr.state.resource.enumerateNetworkStatistics((desc, stats) -> pr.sampleStatistics(samplingSlot, desc, stats));
                }
                catch (Exception e)
                {
                    // Ignore failures.
                }
            }
        }

        queueNextPerfCounterSampling(samplingToken);
    }

    //--//

    @Override
    protected CompletableFuture<Boolean> reloadInner(GatewayState.PersistedNetworkConfiguration networkCfg) throws
                                                                                                            Exception
    {
        boolean success = true;

        for (GatewayState.PersistedProtocolConfiguration protocolCfg : networkCfg.protocols)
        {
            if (protocolCfg.stateFileId != null)
            {
                for (ProtocolRegistration pr : m_protocols)
                {
                    if (pr.state != null && pr.clzConfig == protocolCfg.target.getClass())
                    {
                        success &= await(pr.state.resource.reload((stateProcessor) -> gatewayState.loadState(protocolCfg, stateProcessor)));
                    }
                }
            }
        }

        return wrapAsync(success);
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

        for (ProtocolRegistration pr : m_protocols)
        {
            success &= await(enumerateProtocol(holder_network, en_network, pr.selector, (holder_protocol, en_protocol) ->
            {
                boolean successSub = true;

                if (pr.state != null)
                {
                    successSub &= await(pr.state.resource.discover(operationContext, holder_protocol, broadcastIntervals, rebroadcastCount));
                }

                return wrapAsync(successSub);
            }));
        }

        return wrapAsync(success);
    }

    //--//

    @Override
    public CompletableFuture<Boolean> listObjects(GatewayOperationTracker.State operationContext,
                                                  GatewayState.ResultHolder holder_network,
                                                  GatewayDiscoveryEntity en_network) throws
                                                                                     Exception
    {
        boolean success = true;

        for (ProtocolRegistration pr : m_protocols)
        {
            success &= await(enumerateProtocol(holder_network, en_network, pr.selector, (holder_protocol, en_protocol) ->
            {
                if (pr.state != null)
                {
                    await(pr.state.resource.listObjects(operationContext, holder_protocol, en_protocol));
                }

                return wrapAsync(true);
            }));
        }

        return wrapAsync(success);
    }

    @Override
    public CompletableFuture<Boolean> readAllValues(GatewayOperationTracker.State operationContext,
                                                    GatewayState.ResultHolder holder_network,
                                                    GatewayDiscoveryEntity en_network) throws
                                                                                       Exception
    {
        boolean success = true;

        for (ProtocolRegistration pr : m_protocols)
        {
            success &= await(enumerateProtocol(holder_network, en_network, pr.selector, (holder_protocol, en_protocol) ->
            {
                if (pr.state != null)
                {
                    await(pr.state.resource.readAllValues(operationContext, holder_protocol, en_protocol));
                }

                return wrapAsync(true);
            }));
        }

        return wrapAsync(success);
    }

    @Override
    public CompletableFuture<Boolean> writeValues(GatewayOperationTracker.State operationContext,
                                                  GatewayState.ResultHolder holder_network,
                                                  GatewayDiscoveryEntity en_network) throws
                                                                                     Exception
    {
        boolean success = true;

        for (ProtocolRegistration pr : m_protocols)
        {
            success &= await(enumerateProtocol(holder_network, en_network, pr.selector, (holder_protocol, en_protocol) ->
            {
                if (pr.state != null)
                {
                    await(pr.state.resource.writeValues(operationContext, holder_protocol, en_protocol));
                }

                return wrapAsync(true);
            }));
        }

        return wrapAsync(success);
    }

    //--//

    @Override
    public CompletableFuture<Boolean> startSamplingConfiguration(GatewayOperationTracker.State operationContext,
                                                                 GatewayState.ResultHolder holder_network,
                                                                 GatewayDiscoveryEntity en_network) throws
                                                                                                    Exception
    {
        boolean success = true;

        for (ProtocolRegistration pr : m_protocols)
        {
            success &= await(enumerateProtocol(holder_network, en_network, pr.selector, (holder_protocol, en_protocol) ->
            {
                if (pr.state != null)
                {
                    await(pr.state.resource.startSamplingConfiguration(operationContext));
                }

                return wrapAsync(true);
            }));
        }

        return wrapAsync(success);
    }

    @Override
    public CompletableFuture<Boolean> updateSamplingConfiguration(GatewayOperationTracker.State operationContext,
                                                                  GatewayState.ResultHolder holder_network,
                                                                  GatewayDiscoveryEntity en_network) throws
                                                                                                     Exception
    {
        boolean success = true;

        for (ProtocolRegistration pr : m_protocols)
        {
            success &= await(enumerateProtocol(holder_network, en_network, pr.selector, (holder_protocol, en_protocol) ->
            {
                if (pr.state != null)
                {
                    await(pr.state.resource.updateSamplingConfiguration(operationContext, en_protocol));
                }

                return wrapAsync(true);
            }));
        }

        return wrapAsync(success);
    }

    @Override
    public CompletableFuture<Boolean> completeSamplingConfiguration(GatewayOperationTracker.State operationContext,
                                                                    GatewayState.ResultHolder holder_network,
                                                                    GatewayDiscoveryEntity en_network) throws
                                                                                                       Exception
    {
        boolean success = true;

        for (ProtocolRegistration pr : m_protocols)
        {
            success &= await(enumerateProtocol(holder_network, en_network, pr.selector, (holder_protocol, en_protocol) ->
            {
                if (pr.state != null)
                {
                    await(pr.state.resource.completeSamplingConfiguration(operationContext, en_protocol.contents));
                }

                return wrapAsync(true);
            }));
        }

        return wrapAsync(success);
    }
}