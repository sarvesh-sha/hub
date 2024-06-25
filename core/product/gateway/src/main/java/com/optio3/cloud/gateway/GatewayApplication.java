/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.gateway;

import java.io.File;
import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.optio3.cloud.AbstractApplication;
import com.optio3.cloud.JsonDatagram;
import com.optio3.cloud.authentication.WellKnownRole;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.cloud.client.gateway.orchestration.state.GatewayOperationTracker;
import com.optio3.cloud.client.gateway.orchestration.state.GatewayState;
import com.optio3.cloud.gateway.logic.Heartbeat;
import com.optio3.cloud.gateway.logic.ProberOperationTracker;
import com.optio3.cloud.gateway.logic.SamplePerfCounters;
import com.optio3.cloud.gateway.orchestration.state.GatewayStateImpl;
import com.optio3.cloud.messagebus.MessageBusClient;
import com.optio3.cloud.messagebus.MessageBusClientDatagram;
import com.optio3.cloud.messagebus.MessageBusClientWebSocket;
import com.optio3.cloud.messagebus.MessageBusStatistics;
import com.optio3.cloud.messagebus.channel.RpcClient;
import com.optio3.cloud.messagebus.channel.RpcContext;
import com.optio3.cloud.messagebus.channel.RpcWorker;
import com.optio3.cloud.messagebus.payload.MbControl_UpgradeToUDP;
import com.optio3.cloud.messagebus.payload.MbControl_UpgradeToUDP_Reply;
import com.optio3.cloud.remoting.CallMarshaller;
import com.optio3.infra.NetworkHelper;
import com.optio3.infra.waypoint.FirmwareHelper;
import com.optio3.logging.ILogger;
import com.optio3.logging.ILoggerAppender;
import com.optio3.logging.Logger;
import com.optio3.logging.LoggerFactory;
import com.optio3.logging.Severity;
import com.optio3.util.ConfigurationPersistenceHelper;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import com.sun.jna.Platform;
import io.dropwizard.setup.Bootstrap;
import org.glassfish.jersey.internal.inject.InjectionManager;

public class GatewayApplication extends AbstractApplication<GatewayConfiguration>
{
    public static class PersistedState
    {
        public MbControl_UpgradeToUDP_Reply udpUpgrade;
        public List<InetAddress>            addresses;
    }

    static class PersistedStateHolder
    {
        private final ConfigurationPersistenceHelper m_helper;
        private final File                           m_location;

        PersistedState state;

        public PersistedStateHolder(String scratchDirectory,
                                    String instanceId)
        {
            if (scratchDirectory != null)
            {
                Path configRoot = Paths.get(scratchDirectory, "Status");
                m_helper = new ConfigurationPersistenceHelper(configRoot.toFile(), instanceId);
            }
            else
            {
                m_helper = new ConfigurationPersistenceHelper((String) null, null);
            }

            m_location = m_helper.getFile("application-status");
            state      = m_helper.deserializeFromFileNoThrow(m_location, PersistedState.class);
        }

        public PersistedState ensureState()
        {
            if (state == null)
            {
                state = new PersistedState();
            }

            return state;
        }

        public void flush()
        {
            if (m_location != null)
            {
                try
                {
                    if (state != null)
                    {
                        m_helper.serializeToFile(m_location, state);
                    }
                    else
                    {
                        m_location.delete();
                    }
                }
                catch (Throwable t)
                {
                    // Ignore failures.
                }
            }
        }

        //--//

        public void configureUDP(MbControl_UpgradeToUDP_Reply res,
                                 List<InetAddress> addresses)
        {
            var state = ensureState();
            state.udpUpgrade = res;
            state.addresses  = addresses;

            flush();
        }

        public void invalidateUDP()
        {
            var state = ensureState();
            state.udpUpgrade = null;
            state.addresses  = null;

            flush();
        }
    }

    //--//

    public static final Logger LoggerInstance = new Logger(GatewayApplication.class);

    //--//

    private PersistedStateHolder m_persistedStateHolder;

    private RpcWorker m_rpcWorker;

    private final GatewayOperationTracker m_operationTracker          = new GatewayOperationTracker();
    private final ProberOperationTracker  m_operationTrackerForProber = new ProberOperationTracker();

    private GatewayStateImpl m_state;

    private SamplePerfCounters m_sampler;

    private boolean        m_usesCellular;
    private MonotonousTime m_nextCellularCheck;

    //--//

    public static void main(String[] args) throws
                                           Exception
    {
        new GatewayApplication().run(args);
    }

    public GatewayApplication()
    {
        enableVariableSubstition = true;

        //--//

        LoggerFactory.registerAppender(new ILoggerAppender()
        {
            @Override
            public boolean append(ILogger context,
                                  ZonedDateTime timestamp,
                                  Severity level,
                                  String thread,
                                  String selector,
                                  String msg)
            {
                boolean append = false;

                if (!context.canForwardToRemote())
                {
                    // This logger doesn't go to the Hub, send output to the console.
                    append = true;
                }

                switch (level)
                {
                    case Error:
                        // Only send errors to the console.
                        append = true;
                        break;
                }

                return !append;
            }
        });
    }

    @Override
    public String getName()
    {
        return "Optio3 Gateway";
    }

    @Override
    protected void initialize()
    {
        Bootstrap<?> bootstrap = getServiceNonNull(Bootstrap.class);
        bootstrap.addCommand(new GatewayCommand(this));
    }

    @Override
    protected boolean enablePeeringProtocol()
    {
        // We act as a client, no need for peering.
        return false;
    }

    @Override
    protected void run() throws
                         Exception
    {
        GatewayConfiguration cfg = getServiceNonNull(GatewayConfiguration.class);

        m_persistedStateHolder = new PersistedStateHolder(cfg.scratchDirectory, cfg.instanceId);

        //--//

        ConfigurationPersistenceHelper persistenceHelper;

        if (cfg.persistenceDirectory != null)
        {
            persistenceHelper = new ConfigurationPersistenceHelper(cfg.persistenceDirectory, cfg.instanceId);
        }
        else
        {
            persistenceHelper = new ConfigurationPersistenceHelper((String) null, null);
        }

        m_state = new GatewayStateImpl(this, persistenceHelper, cfg.batchPeriodInSeconds, cfg.flushToDiskDelayInSeconds);

        LoggerFactory.registerAppender(new ILoggerAppender()
        {
            @Override
            public boolean append(ILogger context,
                                  ZonedDateTime timestamp,
                                  Severity level,
                                  String thread,
                                  String selector,
                                  String msg) throws
                                              Exception
            {
                if (context.canForwardToRemote())
                {
                    m_state.publishLogEntry(context, timestamp, level, thread, selector, msg);

                    return true; // Done, stop propagating entry.
                }

                return false; // Allow other appenders to see entry.
            }
        });

        LoggerInstance.info("###########################");
        LoggerInstance.info("###########################");
        LoggerInstance.info("Starting Gateway...");

        discoverRemotableEndpoints("com.optio3.cloud.gateway.remoting.impl.");

        m_state.reloadConfiguration();
    }

    @Override
    public void cleanupOnShutdown(long timeout,
                                  TimeUnit unit)
    {
        LoggerInstance.error("Initiating shutdown...");
        m_state.shutdown();

        LoggerInstance.error("Completed shutdown.");
        stopLoop();
    }

    //--//

    void startLoop()
    {
        if (m_rpcWorker != null)
        {
            throw new RuntimeException("Already started");
        }

        GatewayConfiguration cfg = getServiceNonNull(GatewayConfiguration.class);
        m_sampler = new SamplePerfCounters(LoggerInstance, cfg.instanceId, Math.max(10, cfg.samplingPeriodForPerformanceCounters))
        {
            @Override
            protected int getNumberOfConnections()
            {
                return m_rpcWorker.getNumberOfConnections();
            }

            @Override
            protected int getNumberOfPendingEntities()
            {
                return m_state.getNumberOfPendingEntities();
            }

            @Override
            protected long getNumberOfUploadedEntities()
            {
                return m_state.getNumberOfUploadedEntities();
            }

            @Override
            protected long getNumberOfUploadedEntitiesRetries()
            {
                return m_state.getNumberOfUploadedEntitiesRetries();
            }

            @Override
            protected MessageBusStatistics sampleMessageBusStatistics()
            {
                return m_rpcWorker.sampleMessageBusStatistics();
            }

            @Override
            protected GatewayState.ResultHolder getRoot(long timeEpochSeconds)
            {
                return m_state.getRoot(timeEpochSeconds);
            }

            @Override
            protected void notifyLowBattery() throws
                                              Exception
            {
                m_state.startFlushingOfEntities(true);
            }
        };

        RpcContext rpcContext = new RpcContext()
        {
            @Override
            public CallMarshaller getCallMarshaller()
            {
                return getServiceNonNull(CallMarshaller.class);
            }

            @Override
            public InjectionManager getInjectionManager()
            {
                return getServiceNonNull(InjectionManager.class);
            }
        };

        m_rpcWorker = new RpcWorker(rpcContext)
        {
            private JsonDatagram.SessionConfiguration m_sessionForUDP;

            @Override
            protected void setRpcContext(RpcClient rpcClient,
                                         BaseHeartbeat hb)
            {
                GatewayApplication.this.setRpcClient(rpcClient);
                GatewayApplication.this.setRpcHeartbeat(hb);
            }

            @Override
            protected boolean isCellularConnection()
            {
                return NetworkHelper.isCellularConnection();
            }

            @Override
            protected MessageBusClient allocateSocket()
            {
                if (m_sessionForUDP != null)
                {
                    if (!m_sessionForUDP.isValid())
                    {
                        m_sessionForUDP = null;
                        m_persistedStateHolder.invalidateUDP();
                    }
                    else if (m_sessionForUDP.hasActivity())
                    {
                        return new MessageBusClientDatagram(m_sessionForUDP);
                    }
                }

                GatewayConfiguration cfg = getServiceNonNull(GatewayConfiguration.class);

                return new MessageBusClientWebSocket(cfg.dnsHints, getConnectionUrl(), WellKnownRole.Machine, cfg.instanceId)
                {
                    @Override
                    public boolean shouldUpgrade()
                    {
                        var state = m_persistedStateHolder.ensureState();
                        return state.udpUpgrade != null;
                    }

                    @Override
                    public boolean prepareUpgrade(RpcWorker rpcWorker)
                    {
                        if (m_sessionForUDP == null)
                        {
                            var state = m_persistedStateHolder.ensureState();

                            m_sessionForUDP       = new JsonDatagram.SessionConfiguration(state.udpUpgrade, true);
                            m_sessionForUDP.hosts = state.addresses;
                        }

                        if (!m_sessionForUDP.hasActivity())
                        {
                            // Every N hours, try connecting through WebSocket.
                            m_sessionForUDP.markActivity();
                            return false;
                        }

                        return rpcWorker.prepareUpgrade(m_sessionForUDP);
                    }

                    @Override
                    protected MbControl_UpgradeToUDP completeUpgradeRequest(MbControl_UpgradeToUDP req)
                    {
                        DockerImageArchitecture arch = FirmwareHelper.architecture();
                        if (arch == null)
                        {
                            LoggerInstance.info("Unrecognized platform %s, using regular HTTPS transport...", Platform.ARCH);
                            return null;
                        }

                        req.isIntel       = arch.isIntel();
                        req.isARM         = arch.isArm();
                        req.registerWidth = arch.getRegisterWidth();

                        GatewayConfiguration cfg = getServiceNonNull(GatewayConfiguration.class);
                        req.hostId = cfg.instanceId;

                        if (req.isARM && !isCellularConnection())
                        {
                            // Not on a cellular connection, use HTTPS transport.
                            LoggerInstance.info("Not on Cellular, using regular HTTPS transport...");
                            return null;
                        }

                        return req;
                    }

                    @Override
                    protected void completeUpgradeResponse(MbControl_UpgradeToUDP_Reply res,
                                                           List<InetAddress> addresses)
                    {
                        m_persistedStateHolder.configureUDP(res, addresses);
                    }
                };
            }

            @Override
            protected String getConnectionUrl()
            {
                GatewayConfiguration cfg = getServiceNonNull(GatewayConfiguration.class);

                return cfg.connectionUrl;
            }

            @Override
            protected BaseHeartbeat<GatewayApplication, GatewayConfiguration> createHeartbeat()
            {
                return new Heartbeat(GatewayApplication.this, this, () ->
                {
                    m_operationTracker.purgeStaleEntries();
                }, null);
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
                m_sessionForUDP = session;
                return true;
            }
        };

        m_rpcWorker.startLoop();
    }

    void stopLoop()
    {
        if (m_sampler != null)
        {
            m_sampler.close();
            m_sampler = null;
        }

        if (m_rpcWorker != null)
        {
            m_rpcWorker.stopLoop();
            m_rpcWorker = null;
        }
    }

    //--//

    public boolean isCellularConnection()
    {
        if (!m_usesCellular)
        {
            if (TimeUtils.isTimeoutExpired(m_nextCellularCheck))
            {
                // Once we detect a cellular connection, we assume we still have one.
                m_usesCellular      = NetworkHelper.isCellularConnection();
                m_nextCellularCheck = TimeUtils.computeTimeoutExpiration(5, TimeUnit.MINUTES);
            }
        }

        return m_usesCellular;
    }

    public GatewayOperationTracker getOperationTracker()
    {
        return m_operationTracker;
    }

    public ProberOperationTracker getOperationTrackerForProber()
    {
        return m_operationTrackerForProber;
    }
}
