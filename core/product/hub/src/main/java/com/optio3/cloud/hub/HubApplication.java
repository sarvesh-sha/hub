/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub;

import java.io.IOException;
import java.net.SocketException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.validation.constraints.NotNull;
import javax.ws.rs.core.GenericType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.AbstractApplicationWithDatabase;
import com.optio3.cloud.authentication.WellKnownRole;
import com.optio3.cloud.authentication.WellKnownRoleIds;
import com.optio3.cloud.authentication.jwt.CookiePrincipal;
import com.optio3.cloud.authentication.jwt.CookiePrincipalRoleResolver;
import com.optio3.cloud.client.builder.model.DeviceDetails;
import com.optio3.cloud.client.builder.model.EmailMessage;
import com.optio3.cloud.client.builder.model.TextMessage;
import com.optio3.cloud.db.Optio3DataSourceFactory;
import com.optio3.cloud.db.Optio3DbRateLimiter;
import com.optio3.cloud.hub.demo.DataLoader;
import com.optio3.cloud.hub.logic.alerts.AlertExecutionSpooler;
import com.optio3.cloud.hub.logic.location.LocationsEngine;
import com.optio3.cloud.hub.logic.metrics.MetricsBindingSpooler;
import com.optio3.cloud.hub.logic.protocol.CommonProtocolHandlerForIngestion;
import com.optio3.cloud.hub.logic.protocol.IpnHandlerForBatchIngestion;
import com.optio3.cloud.hub.logic.samples.SamplesCache;
import com.optio3.cloud.hub.logic.simulator.remoting.Optio3SimulatedGateway;
import com.optio3.cloud.hub.logic.simulator.remoting.SimulatedGatewayProvider;
import com.optio3.cloud.hub.logic.spooler.ResultStagingSpooler;
import com.optio3.cloud.hub.logic.tags.TagsEngine;
import com.optio3.cloud.hub.model.config.PrivateValue;
import com.optio3.cloud.hub.model.config.PrivateValues;
import com.optio3.cloud.hub.model.customization.InstanceConfiguration;
import com.optio3.cloud.hub.orchestration.tasks.TaskForNotification;
import com.optio3.cloud.hub.persistence.BackgroundActivityChunkRecord;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.hub.persistence.DistributedLockingRecord;
import com.optio3.cloud.hub.persistence.FixupProcessingRecord;
import com.optio3.cloud.hub.persistence.HostAssetRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.config.RoleRecord;
import com.optio3.cloud.hub.persistence.config.SystemPreferenceRecord;
import com.optio3.cloud.hub.persistence.config.SystemPreferenceTypedValue;
import com.optio3.cloud.hub.persistence.config.UserGroupRecord;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.logic.BackgroundActivityScheduler;
import com.optio3.cloud.logic.BackgroundActivityStatus;
import com.optio3.cloud.messagebus.MessageBusBroker;
import com.optio3.cloud.messagebus.channel.RpcChannel;
import com.optio3.cloud.messagebus.channel.RpcClient;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.scheduler.BackgroundActivityFilterPair;
import com.optio3.cloud.model.scheduler.BackgroundActivityFilterRequest;
import com.optio3.cloud.persistence.EncryptedPayload;
import com.optio3.cloud.persistence.LogEntry;
import com.optio3.cloud.persistence.LogHolder;
import com.optio3.cloud.persistence.MetadataField;
import com.optio3.cloud.persistence.RecordForBackgroundActivity;
import com.optio3.cloud.persistence.RecordForFixupProcessing;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.RecordWithMetadata;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.concurrency.AsyncGate;
import com.optio3.concurrency.DebouncedAction;
import com.optio3.concurrency.Executors;
import com.optio3.logging.ConsoleAppender;
import com.optio3.logging.ILogger;
import com.optio3.logging.ILoggerAppender;
import com.optio3.logging.Logger;
import com.optio3.logging.LoggerFactory;
import com.optio3.logging.LoggerResource;
import com.optio3.logging.Severity;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.BoxingUtils;
import com.optio3.util.CollectionUtils;
import com.optio3.util.ConfigVariables;
import com.optio3.util.IConfigVariable;
import com.optio3.util.MonotonousTime;
import com.optio3.util.Resources;
import com.optio3.util.TimeUtils;
import com.optio3.util.function.BiFunctionWithException;
import com.optio3.util.function.FunctionWithException;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.setup.Environment;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.InjectionResolver;

public class HubApplication extends AbstractApplicationWithDatabase<HubConfiguration>
{
    public static class FixupForMetadata extends RecordForFixupProcessing.Handler
    {
        @Override
        public RecordForFixupProcessing.Handler.Result process(Logger logger,
                                                               SessionHolder sessionHolder) throws
                                                                                            Exception
        {
            SessionProvider sessionProvider = sessionHolder.getSessionProvider();

            HubConfiguration cfg = sessionHolder.getServiceNonNull(HubConfiguration.class);
            if (cfg.developerSettings.developerMode)
            {
                // Likely a developer instance, ignore migration.
            }
            else
            {
                // This is going to take a long time, we have a runtime fixup as well, let it run in the background.
                Executors.getDefaultLongRunningThreadPool()
                         .queue(() -> processInBackground(logger, sessionProvider));
            }

            return Result.Done;
        }

        private void processInBackground(Logger logger,
                                         SessionProvider sessionProvider) throws
                                                                          Exception
        {
            Executors.safeSleep(Duration.ofMinutes(10)
                                        .toMillis());

            try (SessionHolder sessionHolder = sessionProvider.newSessionWithTransaction())
            {
                RecordWithMetadata.fixupFormat(logger, sessionHolder);
            }
        }
    }

    //--//

    public enum EmailFlavor
    {
        Info,
        Warning,
        Alert
    }

    public enum ConfigVariable implements IConfigVariable
    {
        HostId("HOST_ID"),
        BuildId("BUILD_ID"),
        RestConnectionUrl("REST_CONNECTION_URL"),
        Timestamp("TIME");

        private final String m_variable;

        ConfigVariable(String variable)
        {
            m_variable = variable;
        }

        public String getVariable()
        {
            return m_variable;
        }
    }

    private static final ConfigVariables.Validator<ConfigVariable> s_configValidator  = new ConfigVariables.Validator<>(ConfigVariable.values());
    private static final ConfigVariables.Template<ConfigVariable>  s_template_startup = s_configValidator.newTemplate(HubApplication.class, "emails/startup.txt", "${", "}");

    //--//

    private List<Class<?>> m_entities;

    private BackgroundActivityScheduler<BackgroundActivityRecord, BackgroundActivityChunkRecord, HostAssetRecord> m_bgScheduler;
    private InstanceConfiguration                                                                                 m_instanceConfiguration;
    private AlertExecutionSpooler                                                                                 m_alertExecution;
    private MetricsBindingSpooler                                                                                 m_metricsBindingSpooler;
    private LocationsEngine                                                                                       m_locationsEngine;
    private ResultStagingSpooler                                                                                  m_resultStaging;
    private SamplesCache                                                                                          m_samplesCache;
    private TagsEngine                                                                                            m_tagsEngine;

    private final DebouncedAction<Void> m_logFlusher = new DebouncedAction<>(() ->
                                                                             {
                                                                                 flushLogEntries(false);

                                                                                 return AsyncRuntime.NullResult;
                                                                             });

    private final LinkedList<LogEntry> m_logEntries = new LinkedList<>();
    private       boolean              m_logRedirectToDB;
    private       boolean              m_logRedirectedToDB;

    public static void main(String[] args) throws
                                           Exception
    {
        new HubApplication().run(args);
    }

    public HubApplication()
    {
        enableVariableSubstition = true;

        registerService(InstanceConfiguration.class, () -> m_instanceConfiguration);
    }

    @Override
    public String getName()
    {
        return "Optio3 Hub";
    }

    @Override
    protected void initialize()
    {
        super.initialize();

        enableAuthentication(new CookiePrincipalRoleResolver()
        {
            @Override
            public boolean stillValid(@NotNull CookiePrincipal principal)
            {
                // TODO: Check if the token refers to LDAP and/or version is unchanged.
                return true;
            }

            @Override
            public boolean hasRole(@NotNull CookiePrincipal principal,
                                   String role)
            {
                HubConfiguration cfg = getServiceNonNull(HubConfiguration.class);

                try (SessionHolder holder = SessionHolder.createWithNewSessionWithoutTransaction(HubApplication.this, null, Optio3DbRateLimiter.Normal))
                {
                    UserRecord rec_user = cfg.userLogic.findUser(holder, principal, true);
                    if (rec_user != null)
                    {
                        return rec_user.hasRole(role);
                    }

                    return false;
                }
            }

            @Override
            public Set<String> getRoles(@NotNull CookiePrincipal principal)
            {
                Set<String> res = Sets.newHashSet();

                HubConfiguration cfg = getServiceNonNull(HubConfiguration.class);

                try (SessionHolder holder = SessionHolder.createWithNewSessionWithoutTransaction(HubApplication.this, null, Optio3DbRateLimiter.Normal))
                {
                    UserRecord rec_user = cfg.userLogic.findUser(holder, principal, true);
                    if (rec_user != null)
                    {
                        for (RoleRecord rec_role : rec_user.getRoles())
                            res.add(rec_role.getName());
                    }

                    return res;
                }
            }

            @Override
            public boolean authenticate(@NotNull CookiePrincipal principal,
                                        String password)
            {
                HubConfiguration cfg = getServiceNonNull(HubConfiguration.class);

                try (SessionHolder holder = SessionHolder.createWithNewSessionWithoutTransaction(HubApplication.this, null, Optio3DbRateLimiter.Normal))
                {
                    return cfg.userLogic.authenticate(holder, principal, password, false);
                }
            }
        });

        //--//

        m_bgScheduler = new BackgroundActivityScheduler<>(this, BackgroundActivityRecord.class)
        {
            @Override
            protected void initialize(SessionHolder holder)
            {
                HubConfiguration cfg = getServiceNonNull(HubConfiguration.class);
                if (!cfg.noBackgroundProcessingDefragmentation)
                {
                    Optio3DataSourceFactory dataFactory = getDataSourceFactory(null);
                    dataFactory.queueDefragmentationAtBoot(m_entities);
                }
            }

            @Override
            protected HostAssetRecord getWorker(SessionHolder sessionHolder)
            {
                return getCurrentHost(sessionHolder);
            }

            @Override
            protected ZonedDateTime findNextActivation(RecordHelper<BackgroundActivityRecord> helper)
            {
                return BackgroundActivityRecord.findNextActivation(helper);
            }

            @Override
            protected TypedRecordIdentityList<BackgroundActivityRecord> listActivities(RecordHelper<BackgroundActivityRecord> helper,
                                                                                       HostAssetRecord hostAffinity,
                                                                                       Collection<BackgroundActivityStatus> filterStatus)
            {
                BackgroundActivityFilterRequest filters = new BackgroundActivityFilterRequest();
                filters.sortBy = RecordForBackgroundActivity.SortByNextActivation;

                if (filterStatus != null)
                {
                    filters.statusFilter = BackgroundActivityFilterPair.build(filterStatus);
                }

                return BackgroundActivityRecord.list(helper, hostAffinity, filters);
            }

            @Override
            protected TypedRecordIdentityList<BackgroundActivityRecord> listReadyActivities(RecordHelper<BackgroundActivityRecord> helper)
            {
                BackgroundActivityFilterRequest filters = new BackgroundActivityFilterRequest();
                filters.onlyReadyToGo = true;
                filters.sortBy        = RecordForBackgroundActivity.SortByNextActivation;
                filters.statusFilter  = BackgroundActivityFilterPair.build(RecordForBackgroundActivity.ReadySet);

                return BackgroundActivityRecord.list(helper, null, filters);
            }

            @Override
            public <S> S getService(Class<S> serviceClass)
            {
                return HubApplication.this.getServiceNonNull(serviceClass);
            }
        };
    }

    @Override
    protected void registerWithJersey(JerseyEnvironment jersey) throws
                                                                Exception
    {
        HubConfiguration cfg = getServiceNonNull(HubConfiguration.class);
        m_logRedirectToDB = !cfg.developerSettings.unitTestMode;
        if (m_logRedirectToDB)
        {
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
                        publishLogEntry(timestamp, level, thread, selector, msg);
                    }

                    // Keep sending output to console until DB is up or developer asked for it.
                    if (!m_logRedirectedToDB || cfg.developerSettings.forceLogToConsole)
                    {
                        return false; // Allow other appenders to see entry.
                    }

                    return true; // Done, stop propagating entry.
                }
            });
        }

        super.registerWithJersey(jersey);

        //--//

        jersey.register(new AbstractBinder()
        {
            @Override
            protected void configure()
            {
                bind(new SimulatedGatewayProvider()).to(new GenericType<InjectionResolver<Optio3SimulatedGateway>>()
                {
                });
            }
        });
    }

    @Override
    protected void initializeDatabase(HubConfiguration cfg,
                                      Environment environment) throws
                                                               Exception
    {
        m_entities = enableHibernate(cfg, environment, null, cfg.getDataSourceFactory(), "com.optio3.cloud.hub.persistence.");
        if (m_entities != null)
        {
            DistributedLockingRecord.registerAsLockProvider(this, m_entities);
        }

        if (cfg.exports != null)
        {
            for (HubExportDefinition exportDefinition : cfg.exports)
            {
                enableHibernate(cfg, environment, exportDefinition.getId(), exportDefinition.getDataSourceFactory(), exportDefinition.getPackageName());
            }
        }
    }

    @Override
    public AsyncGate getGate(Class<? extends AbstractApplicationWithDatabase.GateClass> gateClass)
    {
        if (gateClass == MetricsBindingSpooler.Gate.class)
        {
            return m_metricsBindingSpooler.gate;
        }

        if (gateClass == ResultStagingSpooler.Gate.class)
        {
            return m_resultStaging.gate;
        }

        if (gateClass == TagsEngine.Gate.class)
        {
            return m_tagsEngine.gate;
        }

        return super.getGate(gateClass);
    }

    @Override
    protected boolean enablePeeringProtocol()
    {
        return true;
    }

    @Override
    protected void run() throws
                         Exception
    {
        HubConfiguration cfg = getServiceNonNull(HubConfiguration.class);
        ensureCurrentHost(cfg);

        if (m_logRedirectToDB)
        {
            if (!cfg.developerSettings.forceLogToConsole)
            {
                // Let the developer know what's going to happen...
                System.out.println();
                System.out.println("################# Redirecting output to database...");
                System.out.println();
            }

            m_logRedirectedToDB = true;
            m_logFlusher.schedule(200, TimeUnit.MILLISECONDS);
        }

        m_resultStaging         = new ResultStagingSpooler(this);
        m_tagsEngine            = new TagsEngine(this);
        m_locationsEngine       = new LocationsEngine(this);
        m_samplesCache          = new SamplesCache(this);
        m_metricsBindingSpooler = new MetricsBindingSpooler(this);
        m_alertExecution        = new AlertExecutionSpooler(this);

        //--//

        discoverResources("com.optio3.cloud.hub.api.");
        discoverWebSockets("/api", "com.optio3.cloud.hub.");
        discoverRemotableEndpoints("com.optio3.cloud.hub.remoting.impl.");
        discoverExtraModels("com.optio3.cloud.hub.engine.");
        discoverExtraModels("com.optio3.cloud.hub.model.");
        discoverExtraModels("com.optio3.cloud.hub.remoting.");

        discoverMessageBusChannels("com.optio3.cloud.messagebus.channel.");

        final Function<String, Boolean> handler = (path) ->
        {
            if (path.equals("ngsw.json") && cfg.disableServiceWorker)
            {
                return Boolean.FALSE;
            }

            return Boolean.TRUE;
        };

        if (isResourcePresent("/assets/website/dist", "index.html"))
        {
            serveAssets("/assets/website/dist", "/", "index.html", "HubUI", true, handler);
        }
        else
        {
            boolean first = true;

            for (String locale : new String[] { "en-US", "it", "fr", "es", "de" })
            {
                String root = "/assets/website/dist/" + locale;
                if (isResourcePresent(root, "index.html"))
                {
                    if (first)
                    {
                        serveAssets(root, "/", "index.html", "HubUI", true, handler);
                        first = false;
                    }

                    serveAssets(root, "/" + locale, "index.html", "HubUI-" + locale, false, handler);
                }
            }
        }

        serveAssets("/assets/wdc/dist", "/wdc", "index.html", "WebDataConnector", false, null);

        enableSwagger((config) ->
                      {
                          config.setVersion("1.0.0");
                          config.setTitle("Optio3 Hub APIs");
                          config.setDescription("APIs and Definitions for the Optio3 Hub product.");
                      }, (type, model) -> fixupRecordId(m_entities, type, model), "/api", "/api/v1", "com.optio3.cloud.hub.api");

        //--//

        FixupProcessingRecord.executeAllHandlers(this);

        //--//

        //
        // Create the RPC service using a local channel subscriber.
        //
        MessageBusBroker broker     = getServiceNonNull(MessageBusBroker.class);
        RpcChannel       rpcChannel = broker.getChannelProvider(RpcChannel.class);

        RpcClient client = new RpcClient(rpcChannel.getContext());
        broker.registerLocalChannelSubscriber(client);
        setRpcClient(client);

        //--//

        cfg.userLogic = new HubUserLogic(this);
        cfg.userLogic.initialize();

        //--//

        if (cfg.enableMessageBusOverUDP)
        {
            broker.startUdpWorker(cfg.enableMessageBusOverUDPforIntel);
        }

        if (cfg.developerSettings.bulkRenamingInput != null)
        {
            BulkRenaming.generate(this, cfg.developerSettings.bulkRenamingInput, cfg.developerSettings.bulkRenamingOutput);

            Runtime.getRuntime()
                   .exit(0);
        }

        boolean loadDemoData = false;

        try (SessionHolder holder = SessionHolder.createWithNewSessionWithTransaction(this, null, Optio3DbRateLimiter.System))
        {
            if (RoleRecord.findByName(holder, WellKnownRoleIds.Administrator) == null)
            {
                // Database not initialized.
                loadDemoData = cfg.developerSettings.includeDemoData;
            }

            initializeKnownRoles(holder);

            ensureGroup(holder,
                        UserGroupRecord.WellKnownMetadata.reachabilityGroup,
                        "SYS.NOTIFICATION.UNREACHABLE",
                        "Users in this group get notified when a controller is not reachable over the network");

            ensureGroup(holder, UserGroupRecord.WellKnownMetadata.responsivenessGroup, "SYS.NOTIFICATION.UNRESPONSIVE", "Users in this group get notified when a sensor is not sending data");

            holder.commit();
        }

        try (SessionHolder holder = SessionHolder.createWithNewReadOnlySession(this, null, Optio3DbRateLimiter.System))
        {
            Boolean disable = SystemPreferenceRecord.getTypedValue(holder, SystemPreferenceTypedValue.DisableServiceWorker, Boolean.class);
            if (disable != null)
            {
                cfg.disableServiceWorker = disable;
            }
        }

        if (cfg.instanceConfigurationForUnitTest != null)
        {
            try (SessionHolder holder = SessionHolder.createWithNewSessionWithTransaction(this, null, Optio3DbRateLimiter.System))
            {
                HubConfiguration.setInstanceConfiguration(holder, cfg.instanceConfigurationForUnitTest);

                holder.commit();
            }
        }

        if (cfg.data != null)
        {
            for (HubDataDefinition data : cfg.data)
            {
                if (data.isDemo && !loadDemoData)
                {
                    continue;
                }

                try (SessionHolder holder = SessionHolder.createWithNewSessionWithTransaction(this, null, Optio3DbRateLimiter.System))
                {
                    try
                    {
                        DataLoader dl = DataLoader.fetch(getServiceNonNull(ObjectMapper.class), data.resource);
                        LoggerInstance.info("Loading data %s...", data.resource);

                        if (cfg.developerSettings.verboseDemoData)
                        {
                            dl.dump(this);
                        }

                        dl.apply(this, holder, data.loadIfMissing);

                        holder.commitAndBeginNewTransaction();
                    }
                    catch (IOException e)
                    {
                        System.err.printf("Failed to load %s:%n", data.resource);
                        e.printStackTrace(System.err);

                        throw new RuntimeException(e);
                    }

                    holder.commit();
                }
            }
        }

        //
        // Always refresh normalization rules based on instance configuration.
        //
        try (SessionHolder holder = SessionHolder.createWithNewSessionWithTransaction(this, null, Optio3DbRateLimiter.System))
        {
            InstanceConfiguration instanceCfg = HubConfiguration.getInstanceConfigurationNonNull(holder);
            if (instanceCfg.updateNormalizationRules(holder))
            {
                holder.commit();
            }
        }

        //--//

        if (cfg.developerSettings.dumpStagingStatisticsRaw)
        {
            try (SessionHolder holder = SessionHolder.createWithNewReadOnlySession(this, null, Optio3DbRateLimiter.System))
            {
                CommonProtocolHandlerForIngestion.dumpGenericStatistics(holder, LoggerInstance);
            }

            Runtime.getRuntime()
                   .exit(10);
        }

        if (cfg.developerSettings.dumpStagingStatistics)
        {
            try (SessionHolder holder = SessionHolder.createWithNewReadOnlySession(this, null, Optio3DbRateLimiter.System))
            {
                IpnHandlerForBatchIngestion.dumpStatistics(holder, LoggerInstance);
            }

            Runtime.getRuntime()
                   .exit(10);
        }

        if (StringUtils.isNotBlank(cfg.developerSettings.dumpSamplingStatistics))
        {
            var sessionProvider = new SessionProvider(this, null, Optio3DbRateLimiter.System);

            // Flip to compact before statistics.
            if (false)
            {
//                DeviceElementRecord.ArchiveCompactor.LoggerInstance.enable(Severity.Debug);
                DeviceElementRecord.compactAllTimeSeries(sessionProvider, true);
            }

            if (cfg.developerSettings.dumpSamplingStatisticsPerDevice)
            {
                DeviceElementRecord.dumpSampleStatisticsPerDevice(sessionProvider, LoggerInstance, false, cfg.developerSettings.dumpSamplingStatistics);
            }
            else
            {
                DeviceElementRecord.dumpSampleStatistics(sessionProvider,
                                                         LoggerInstance,
                                                         false,
                                                         cfg.developerSettings.dumpSamplingStatistics,
                                                         cfg.developerSettings.dumpSamplingStatisticsOnlyNetworks,
                                                         cfg.developerSettings.dumpSamplingStatisticsOnlyGateways);
            }

            Runtime.getRuntime()
                   .exit(10);
        }

        if (cfg.startBackgroundProcessing)
        {
            startScheduler("com.optio3.cloud.hub.");
        }

        if (cfg.hostId != null)
        {
            ConfigVariables<ConfigVariable> parameters = s_template_startup.allocate();

            parameters.setValue(ConfigVariable.HostId, cfg.hostId);
            parameters.setValue(ConfigVariable.BuildId, cfg.buildId);
            parameters.setValue(ConfigVariable.RestConnectionUrl, cfg.cloudConnectionUrl);
            parameters.setValue(ConfigVariable.Timestamp, TimeUtils.now());

            try (SessionHolder sessionHolder = SessionHolder.createWithNewSessionWithTransaction(this, null, Optio3DbRateLimiter.System))
            {
                sendEmailNotification(sessionHolder, true, EmailFlavor.Info, null, true, parameters);

                sessionHolder.commit();
            }
        }

        //--//

        if (m_instanceConfiguration == null)
        {
            InstanceConfiguration instanceConfiguration;

            try (SessionHolder holder = SessionHolder.createWithNewSessionWithoutTransaction(this, null, Optio3DbRateLimiter.System))
            {
                instanceConfiguration = HubConfiguration.getInstanceConfigurationNonNull(holder);
            }

            setInstanceConfiguration(instanceConfiguration);
        }
    }

    @Override
    protected void onServerStarted()
    {
        super.onServerStarted();

        m_locationsEngine.initialize();
        m_tagsEngine.initialize();
        m_samplesCache.initialize();
        m_alertExecution.initialize();
        m_metricsBindingSpooler.initialize();
        m_resultStaging.initialize();
    }

    @Override
    public void cleanupOnShutdown(long timeout,
                                  TimeUnit unit) throws
                                                 Exception
    {
        stopScheduler(timeout, unit);

        setInstanceConfiguration(null);

        if (m_alertExecution != null)
        {
            m_alertExecution.close();
        }

        if (m_metricsBindingSpooler != null)
        {
            m_metricsBindingSpooler.close();
        }

        if (m_resultStaging != null)
        {
            m_resultStaging.close();
        }

        if (m_tagsEngine != null)
        {
            m_tagsEngine.close();
        }

        if (m_locationsEngine != null)
        {
            m_locationsEngine.close();
        }

        if (m_samplesCache != null)
        {
            m_samplesCache.close();
        }

        if (m_logRedirectedToDB)
        {
            flushLogEntries(true);
        }
    }

    //--//

    public void setInstanceConfiguration(InstanceConfiguration instanceConfiguration) throws
                                                                                      SocketException
    {
        if (m_instanceConfiguration != null)
        {
            m_instanceConfiguration.stop();
            m_instanceConfiguration.setApp(null);
        }

        m_instanceConfiguration = instanceConfiguration;
        if (instanceConfiguration != null)
        {
            instanceConfiguration.setApp(this);
            instanceConfiguration.start();
        }
    }

    public void startScheduler(String packagePrefixForRecurringProcessors)
    {
        m_bgScheduler.start(packagePrefixForRecurringProcessors);
    }

    public void stopScheduler(long timeout,
                              TimeUnit unit)
    {
        m_bgScheduler.stop(timeout, unit);
    }

    public void triggerScheduler()
    {
        m_bgScheduler.trigger();
    }

    //--//

    private MonotonousTime m_suspendResultStagingSpoolerUntil;
    private boolean        m_suspendResultStagingSpoolerUntilReported;

    public void suspendResultStagingSpooler(int amount,
                                            TimeUnit unit)
    {
        MonotonousTime newDeadline = TimeUtils.computeTimeoutExpiration(amount, unit);
        m_suspendResultStagingSpoolerUntil = TimeUtils.updateIfAfter(m_suspendResultStagingSpoolerUntil, newDeadline);

        // Wait up to a minute for the spooler to yield.
        getServiceNonNull(ResultStagingSpooler.class).flushAssets(Collections.emptyList(), Duration.of(1, ChronoUnit.MINUTES));
    }

    public boolean shouldSuspendResultStagingSpooler()
    {
        if (TimeUtils.isTimeoutExpired(m_suspendResultStagingSpoolerUntil))
        {
            m_suspendResultStagingSpoolerUntil         = null;
            m_suspendResultStagingSpoolerUntilReported = false;
        }

        if (m_suspendResultStagingSpoolerUntil == null)
        {
            return false;
        }

        if (!m_suspendResultStagingSpoolerUntilReported)
        {
            m_suspendResultStagingSpoolerUntilReported = true;
            LoggerInstance.debug("Result Staging Spooler suspended until %s...", m_suspendResultStagingSpoolerUntil);
        }

        return true;
    }

    //--//

    public void sendEmailNotification(SessionHolder sessionHolder,
                                      boolean systemGenerated,
                                      EmailFlavor flavor,
                                      String subject,
                                      boolean addSiteUrl,
                                      ConfigVariables parameters)
    {
        HubConfiguration cfg = getServiceNonNull(HubConfiguration.class);

        String emailTo;

        switch (flavor)
        {
            case Info:
                emailTo = cfg.emailForInfo;
                subject = BoxingUtils.get(subject, "Hub Info");
                break;

            case Warning:
                emailTo = cfg.emailForWarnings;
                subject = BoxingUtils.get(subject, "Hub Warning");
                break;

            case Alert:
                emailTo = cfg.emailForAlerts;
                subject = BoxingUtils.get(subject, "Hub Alert");
                break;

            default:
                LoggerInstance.error("Unknown email flavor: %s", flavor);
                return;
        }

        sendEmailNotification(sessionHolder, systemGenerated, emailTo, subject, addSiteUrl, parameters);
    }

    public void sendEmailNotification(SessionHolder sessionHolder,
                                      boolean systemGenerated,
                                      String emailTo,
                                      String subject,
                                      boolean addSiteUrl,
                                      ConfigVariables parameters)
    {
        try
        {
            HubConfiguration cfg       = getServiceNonNull(HubConfiguration.class);
            String           emailText = parameters.convert();

            if (addSiteUrl)
            {
                subject = String.format("%s - %s", subject, cfg.cloudConnectionUrl);
            }

            LoggerInstance.info(cfg.developerSettings.disableEmails ? "Skipping sending email to %s" : "Sending email to %s", emailTo);
            LoggerInstance.info("Subject: %s", subject);

            try (LoggerResource resource = LoggerFactory.indent("Body:  "))
            {
                LoggerInstance.info("%s", emailText);
            }

            if (cfg.developerSettings.disableEmails)
            {
                return;
            }

            EmailMessage message = new EmailMessage();
            message.systemGenerated = systemGenerated;
            message.from            = TaskForNotification.newRecipient("no-reply-hub@optio3.io", null);
            message.to.add(TaskForNotification.newRecipient(emailTo, null));
            message.subject = subject;
            message.text    = emailText;

            TaskForNotification.scheduleTask(sessionHolder, message, null, null, null);
        }
        catch (Throwable t)
        {
            LoggerInstance.error("Failed to send email: %s", t);
        }
    }

    public void sendTextNotification(SessionHolder sessionHolder,
                                     boolean systemGenerated,
                                     String senderId,
                                     String phoneNumber,
                                     ConfigVariables parameters)
    {
        try
        {
            HubConfiguration cfg = getServiceNonNull(HubConfiguration.class);

            if (cfg.developerSettings.disableSMSs)
            {
                return;
            }

            String contents = parameters.convert();

            TextMessage message = new TextMessage();
            message.systemGenerated = systemGenerated;
            message.phoneNumbers.add(phoneNumber);
            message.senderId = senderId;
            message.text     = contents;

            TaskForNotification.scheduleTask(sessionHolder, null, message, null, null);
        }
        catch (Throwable t)
        {
            LoggerInstance.error("Failed to send text: %s", t);
        }
    }

    public void sendDeviceNotification(SessionHolder sessionHolder,
                                       DeviceDetails deviceDetails)
    {
        try
        {
            TaskForNotification.scheduleTask(sessionHolder, null, null, deviceDetails, null);
        }
        catch (Throwable t)
        {
            LoggerInstance.error("Failed to send device notification: %s", t);
        }
    }

    //--//

    public List<PrivateValue> getPrivateValues(String id) throws
                                                          Exception
    {
        try (SessionHolder sessionHolder = SessionHolder.createWithNewReadOnlySession(this, null, Optio3DbRateLimiter.Normal))
        {
            var lst = SystemPreferenceRecord.getTypedSubValue(sessionHolder, SystemPreferenceTypedValue.PrivateValues, id, PrivateValues.class);

            return lst != null ? lst.values : Lists.newArrayList();
        }
    }

    public <V> V computeWithPrivateValue(String id,
                                         BiFunctionWithException<String, String, V> callback) throws
                                                                                              Exception
    {
        var cfg = getServiceNonNull(HubConfiguration.class);

        for (PrivateValue value : getPrivateValues(id))
        {
            String payload;

            if (value.value != null)
            {
                var ep = EncryptedPayload.decodeFromBase64(value.value);
                payload = cfg.decrypt(ep);
            }
            else
            {
                payload = null;
            }

            V res = callback.apply(value.key, payload);
            if (res != null)
            {
                return res;
            }
        }

        return null;
    }

    public <V> V computeWithPrivateValue(String id,
                                         String key,
                                         FunctionWithException<String, V> callback) throws
                                                                                    Exception
    {
        return computeWithPrivateValue(id, (k, v) ->
        {
            if (StringUtils.equals(key, k))
            {
                return callback.apply(v);
            }

            return null;
        });
    }

    public void setPrivateValue(String id,
                                String key,
                                String value) throws
                                              Exception
    {
        var v = new PrivateValue();
        v.key   = key;
        v.value = value;

        setPrivateValues(id, Lists.newArrayList(v));
    }

    public void setPrivateValues(String id,
                                 List<PrivateValue> values) throws
                                                            Exception
    {
        try (SessionHolder sessionHolder = SessionHolder.createWithNewSessionWithTransaction(this, null, Optio3DbRateLimiter.Normal))
        {
            if (CollectionUtils.isNotEmpty(values))
            {
                PrivateValues pv = new PrivateValues();
                pv.values.addAll(values);

                SystemPreferenceRecord.setTypedSubValue(sessionHolder, SystemPreferenceTypedValue.PrivateValues, id, pv);
            }
            else
            {
                SystemPreferenceRecord.removeTypedSubValue(sessionHolder, SystemPreferenceTypedValue.PrivateValues, id);
            }

            sessionHolder.commit();
        }
    }

    public <V> V getTypedPrivateValue(String id,
                                      Class<V> clz) throws
                                                    Exception
    {
        return computeWithPrivateValue(id, (key, value) -> value != null ? ObjectMappers.SkipNulls.readValue(value, clz) : null);
    }

    public void setTypedPrivateValue(String id,
                                     Object obj) throws
                                                 Exception
    {
        List<PrivateValue> values = Lists.newArrayList();

        if (obj != null)
        {
            var cfg = getServiceNonNull(HubConfiguration.class);
            var ep  = cfg.encrypt(ObjectMappers.SkipNulls.writeValueAsString(obj));

            PrivateValue value = new PrivateValue();
            value.value = ep.encodeAsBase64();

            values.add(value);
        }

        setPrivateValues(id, values);
    }

    //--//

    public HostAssetRecord getCurrentHost(SessionHolder sessionHolder)
    {
        HubConfiguration cfg = getServiceNonNull(HubConfiguration.class);

        return sessionHolder.getEntity(HostAssetRecord.class, cfg.hostId);
    }

    private void ensureCurrentHost(HubConfiguration cfg)
    {
        try (SessionHolder holder = SessionHolder.createWithNewSessionWithTransaction(this, null, Optio3DbRateLimiter.Normal))
        {
            HostAssetRecord rec = holder.getEntityOrNull(HostAssetRecord.class, cfg.hostId);
            if (rec == null)
            {
                rec = new HostAssetRecord();
                rec.setSysId(cfg.hostId);
                rec.setPhysicalName(cfg.hostId.replace('_', ' '));
                holder.persistEntity(rec);

                holder.commit();
            }
        }
    }

    private boolean isResourcePresent(String resourcePath,
                                      String indexFile)
    {
        if (!resourcePath.endsWith("/"))
        {
            resourcePath += '/';
        }

        return Resources.openResource(getClass(), resourcePath.substring(1) + indexFile) != null;
    }

    private static void initializeKnownRoles(SessionHolder holder)
    {
        RecordHelper<RoleRecord> helper = holder.createHelper(RoleRecord.class);

        ensureRole(helper, WellKnownRole.Administrator, true, true);
        ensureRole(helper, WellKnownRole.Publisher, true, true);
        ensureRole(helper, WellKnownRole.User, true, true);

        ensureRole(helper, WellKnownRole.Maintenance, false, false);
        ensureRole(helper, WellKnownRole.Infrastructure, false, false);
        ensureRole(helper, WellKnownRole.Machine, false, false);
    }

    private static void ensureRole(RecordHelper<RoleRecord> helper,
                                   WellKnownRole role,
                                   boolean addAllowed,
                                   boolean removeAllowed)
    {
        if (RoleRecord.findByName(helper.currentSessionHolder(), role.getId()) == null)
        {
            RoleRecord rec = new RoleRecord();
            rec.setName(role.getId());
            rec.setDisplayName(role.getDisplayName());
            rec.setAddAllowed(addAllowed);
            rec.setRemoveAllowed(removeAllowed);

            helper.persist(rec);
        }
    }

    private static void ensureGroup(SessionHolder holder,
                                    MetadataField<Boolean> flag,
                                    String name,
                                    String description)
    {
        if (UserGroupRecord.findByMetadata(holder, flag) == null)
        {
            RecordHelper<UserGroupRecord> helper_group = holder.createHelper(UserGroupRecord.class);

            UserGroupRecord rec_newGroup = new UserGroupRecord();
            rec_newGroup.setName(name);
            rec_newGroup.setDescription(description);
            rec_newGroup.putMetadata(flag, true);

            helper_group.persist(rec_newGroup);
        }
    }

    //--//

    private void publishLogEntry(ZonedDateTime timestamp,
                                 Severity level,
                                 String thread,
                                 String selector,
                                 String msg) throws
                                             Exception
    {
        LogEntry en = new LogEntry();
        en.timestamp = timestamp;
        en.level     = level;
        en.thread    = thread;
        en.selector  = selector;
        en.line      = msg;

        synchronized (m_logEntries)
        {
            final int maxPendingEntries = 50_000;

            if (m_logEntries.size() > maxPendingEntries)
            {
                // Avoid running out of memory...
                m_logEntries.removeFirst();
            }

            m_logEntries.add(en);

            if (m_logRedirectedToDB)
            {
                m_logFlusher.schedule(1, TimeUnit.SECONDS);
            }
        }
    }

    public int getPendingLogEntries()
    {
        synchronized (m_logEntries)
        {
            return m_logEntries.size();
        }
    }

    public void flushLogEntries(boolean toConsole) throws
                                                   Exception
    {
        if (toConsole)
        {
            synchronized (m_logEntries)
            {
                var logger = new ConsoleAppender();

                for (LogEntry entry : m_logEntries)
                {
                    logger.append(null, entry.timestamp, entry.level, entry.thread, entry.selector, entry.line);
                }
            }
        }

        HubConfiguration cfg = getServiceNonNull(HubConfiguration.class);

        List<LogEntry> candidates = Lists.newArrayList();

        while (true)
        {
            candidates.clear();

            int queued = 0;

            synchronized (m_logEntries)
            {
                int threshold = 500;

                for (LogEntry entry : m_logEntries)
                {
                    candidates.add(entry);
                    queued++;

                    if (--threshold == 0)
                    {
                        break;
                    }
                }
            }

            if (candidates.size() == 0)
            {
                break;
            }

            try (SessionHolder sessionHolder = SessionHolder.createWithNewSessionWithTransaction(this, null, Optio3DbRateLimiter.HighPriority))
            {
                RecordLocked<HostAssetRecord> lock_host = sessionHolder.getEntityWithLock(HostAssetRecord.class, cfg.hostId, 30, TimeUnit.SECONDS);

                try (var logHandler = HostAssetRecord.allocateLogHandler(lock_host))
                {
                    try (LogHolder log = logHandler.newLogHolder())
                    {
                        for (LogEntry entry : candidates)
                        {
                            log.addLineSync(1, entry.timestamp, null, entry.thread, entry.selector, entry.level, entry.line);
                        }
                    }

                    logHandler.trim(100_000, 20_000, Duration.ofDays(28), Duration.ofDays(2));
                }

                sessionHolder.commit();
            }

            synchronized (m_logEntries)
            {
                for (int i = 0; i < queued && !m_logEntries.isEmpty(); i++)
                {
                    m_logEntries.removeFirst();
                }
            }
        }
    }
}
