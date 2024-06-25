/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.simulator;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.net.InetAddress;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.cloud.JsonDatagram;
import com.optio3.cloud.authentication.WellKnownRole;
import com.optio3.cloud.authentication.jwt.CookiePrincipal;
import com.optio3.cloud.client.gateway.model.GatewayStatus;
import com.optio3.cloud.client.gateway.orchestration.state.GatewayOperationTracker;
import com.optio3.cloud.client.gateway.orchestration.state.GatewayState;
import com.optio3.cloud.client.gateway.proxy.GatewayStatusApi;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.logic.simulator.generators.DeviceGenerator;
import com.optio3.cloud.hub.logic.simulator.remoting.SimulatedGatewayControlApiImpl;
import com.optio3.cloud.hub.logic.simulator.remoting.SimulatedGatewayDiscoveryApiImpl;
import com.optio3.cloud.hub.logic.simulator.remoting.SimulatedGatewayProvider;
import com.optio3.cloud.hub.logic.simulator.state.SimulatedGatewayStateImpl;
import com.optio3.cloud.messagebus.MessageBusClient;
import com.optio3.cloud.messagebus.MessageBusClientWebSocket;
import com.optio3.cloud.messagebus.WellKnownDestination;
import com.optio3.cloud.messagebus.channel.RpcClient;
import com.optio3.cloud.messagebus.channel.RpcContext;
import com.optio3.cloud.messagebus.channel.RpcWorker;
import com.optio3.cloud.messagebus.payload.MbControl_UpgradeToUDP;
import com.optio3.cloud.messagebus.payload.MbControl_UpgradeToUDP_Reply;
import com.optio3.cloud.remoting.CallMarshaller;
import com.optio3.logging.Logger;
import com.optio3.util.TimeUtils;
import org.glassfish.jersey.internal.inject.InjectionManager;

public class SimulatedGateway
{
    public static final Logger LoggerInstance = new Logger(SimulatedGateway.class);

    private static final Map<String, SimulatedGateway> m_simulatedGateways = Maps.newHashMap();

    private final HubApplication            m_app;
    private final String                    m_instanceId;
    private       SimulatedGatewayStateImpl m_state;
    private       ZonedDateTime             m_historicalDataStartDate;

    private GatewayOperationTracker      m_operationTracker = new GatewayOperationTracker();
    private CallMarshaller               m_marshaller       = new CallMarshaller();
    private CompletableFuture<RpcClient> m_rpc              = new CompletableFuture<>();

    private List<DeviceGenerator> m_devices = Lists.newArrayList();

    public SimulatedGateway(HubApplication app,
                            String instanceId,
                            int numHistoricalDays)
    {
        m_app        = app;
        m_instanceId = instanceId;
        if (numHistoricalDays > 0)
        {
            m_historicalDataStartDate = TimeUtils.now()
                                                 .minusDays(numHistoricalDays);
        }

        m_marshaller.addRemotableEndpoint(SimulatedGatewayControlApiImpl.class);
        m_marshaller.addRemotableEndpoint(SimulatedGatewayDiscoveryApiImpl.class);

        RpcContext context = new RpcContext()
        {
            @Override
            public CallMarshaller getCallMarshaller()
            {
                return m_marshaller;
            }

            @Override
            public InjectionManager getInjectionManager()
            {
                SimulatedGatewayProvider.set(SimulatedGateway.this);
                return m_app.getServiceNonNull(InjectionManager.class);
            }
        };

        String token = getWebsocketConnectionToken();

        RpcWorker worker = new RpcWorker(context)
        {
            @Override
            protected void setRpcContext(RpcClient rpcClient,
                                         BaseHeartbeat hb)
            {
                SimulatedGateway.this.setRpcClient(rpcClient);
            }

            @Override
            protected boolean isCellularConnection()
            {
                return false;
            }

            @Override
            protected MessageBusClient allocateSocket()
            {
                return new MessageBusClientWebSocket(null, getConnectionUrl(), token)
                {
                    @Override
                    public boolean shouldUpgrade()
                    {
                        return false;
                    }

                    @Override
                    public boolean prepareUpgrade(RpcWorker rpcWorker)
                    {
                        return false;
                    }

                    @Override
                    protected MbControl_UpgradeToUDP completeUpgradeRequest(MbControl_UpgradeToUDP req)
                    {
                        // No UDP for simulated gateway.
                        return null;
                    }

                    @Override
                    protected void completeUpgradeResponse(MbControl_UpgradeToUDP_Reply res,
                                                           List<InetAddress> addresses)
                    {
                        // Nothing to do.
                    }
                };
            }

            @Override
            protected String getConnectionUrl()
            {
                HubConfiguration cfg = m_app.getServiceNonNull(HubConfiguration.class);
                return getWebsocketConnectionUrl(cfg.cloudConnectionUrl);
            }

            @Override
            protected BaseHeartbeat<HubApplication, HubConfiguration> createHeartbeat()
            {
                return new BaseHeartbeat<>(getApp(), LoggerInstance, this, null, null)
                {
                    @Override
                    protected CompletableFuture<Duration> sendCheckinInner(boolean force) throws
                                                                                          Exception
                    {
                        GatewayStatus status = new GatewayStatus();

                        status.instanceId = m_instanceId;

                        Runtime runtime = Runtime.getRuntime();
                        status.availableProcessors = runtime.availableProcessors();
                        status.maxMemory           = runtime.maxMemory();
                        status.freeMemory          = runtime.freeMemory();
                        status.totalMemory         = runtime.totalMemory();

                        GatewayState state = getState();
                        state.exportNetworks(status.networks);
                        status.queueStatus = state.checkQueueStatus();

                        RpcClient        client = await(getRpcClient());
                        GatewayStatusApi proxy  = client.createProxy(WellKnownDestination.Service.getId(), null, GatewayStatusApi.class, 20, TimeUnit.SECONDS);

                        await(proxy.checkin(status));

                        SimulatedGateway.this.m_operationTracker.purgeStaleEntries();

                        return wrapAsync(null);
                    }
                };
            }

            @Override
            protected void onSuccess()
            {
            }

            @Override
            protected void onFailure()
            {
            }

            @Override
            public boolean prepareUpgrade(JsonDatagram.SessionConfiguration session)
            {
                return false;
            }
        };

        worker.startLoop();
    }

    public static SimulatedGateway create(HubApplication app,
                                          String instanceId,
                                          int numHistoricalDays)
    {
        return m_simulatedGateways.computeIfAbsent(instanceId, id -> new SimulatedGateway(app, instanceId, numHistoricalDays));
    }

    public static SimulatedGateway get(String instanceId)
    {
        return m_simulatedGateways.get(instanceId);
    }

    public ZonedDateTime getHistoricalDataStartDate()
    {
        return m_historicalDataStartDate;
    }

    public void setHistoricalDataStartDate(ZonedDateTime historicalDataStartDate)
    {
        m_historicalDataStartDate = historicalDataStartDate;
    }

    public List<DeviceGenerator> getDevices()
    {
        return m_devices;
    }

    public void addDevice(DeviceGenerator device)
    {
        m_devices.add(device);
    }

    public GatewayOperationTracker getOperationTracker()
    {
        return m_operationTracker;
    }

    public SimulatedGatewayStateImpl getState()
    {
        if (m_state == null)
        {
            m_state = new SimulatedGatewayStateImpl(this);
        }

        return m_state;
    }

    public CompletableFuture<RpcClient> getRpcClient()
    {
        return m_rpc;
    }

    private void setRpcClient(RpcClient client)
    {
        if (client == null)
        {
            m_rpc = new CompletableFuture<>();
        }
        else
        {
            m_rpc.complete(client);
        }
    }

    private String getWebsocketConnectionUrl(String baseUrl)
    {
        baseUrl = baseUrl.replace("http://", "ws://");
        baseUrl = baseUrl.replace("https://", "wss://");
        return baseUrl + "/api/v1/message-bus";
    }

    private String getWebsocketConnectionToken()
    {
        HubApplication hub = getApp();

        CookiePrincipal principal = hub.buildPrincipal("simulatedGateway@local");
        principal.setEmbeddedRolesEx(WellKnownRole.Machine);
        return hub.generateCookie(principal)
                  .toString();
    }

    private HubApplication getApp()
    {
        return m_app;
    }
}


