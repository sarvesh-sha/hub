/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder;

import java.io.File;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import javax.persistence.Tuple;
import javax.validation.constraints.NotNull;

import com.google.common.base.Suppliers;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.AbstractApplicationWithDatabase;
import com.optio3.cloud.authentication.WellKnownRole;
import com.optio3.cloud.authentication.jwt.CookiePrincipal;
import com.optio3.cloud.authentication.jwt.CookiePrincipalRoleResolver;
import com.optio3.cloud.builder.model.communication.EmailMessage;
import com.optio3.cloud.builder.model.deployment.DeploymentCellularCharges;
import com.optio3.cloud.builder.model.deployment.DeploymentGlobalDescriptor;
import com.optio3.cloud.builder.model.deployment.DeploymentHostDetails;
import com.optio3.cloud.builder.model.jobs.JobStatus;
import com.optio3.cloud.builder.model.worker.Host;
import com.optio3.cloud.builder.orchestration.tasks.bookkeeping.TaskForNotification;
import com.optio3.cloud.builder.orchestration.tasks.bookkeeping.TaskForRegistryRefresh;
import com.optio3.cloud.builder.orchestration.tasks.bookkeeping.TaskForRepositoryRefresh;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.builder.persistence.BackgroundActivityChunkRecord;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.builder.persistence.DistributedLockingRecord;
import com.optio3.cloud.builder.persistence.FixupProcessingRecord;
import com.optio3.cloud.builder.persistence.config.RoleRecord;
import com.optio3.cloud.builder.persistence.config.UserRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.customer.DatabaseMode;
import com.optio3.cloud.builder.persistence.customer.EmbeddedDatabaseConfiguration;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.jobs.JobDefinitionRecord;
import com.optio3.cloud.builder.persistence.jobs.JobDefinitionStepRecord;
import com.optio3.cloud.builder.persistence.jobs.JobDefinitionStepRecordForDockerBuild;
import com.optio3.cloud.builder.persistence.jobs.JobDefinitionStepRecordForDockerPush;
import com.optio3.cloud.builder.persistence.jobs.JobDefinitionStepRecordForDockerRun;
import com.optio3.cloud.builder.persistence.jobs.JobDefinitionStepRecordForGit;
import com.optio3.cloud.builder.persistence.jobs.JobDefinitionStepRecordForMaven;
import com.optio3.cloud.builder.persistence.jobs.JobDefinitionStepRecordForSshCommand;
import com.optio3.cloud.builder.persistence.jobs.JobRecord;
import com.optio3.cloud.builder.persistence.jobs.JobStepRecord;
import com.optio3.cloud.builder.persistence.jobs.input.RepositoryRecord;
import com.optio3.cloud.builder.persistence.worker.HostRecord;
import com.optio3.cloud.builder.persistence.worker.ManagedDirectoryRecord;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.cloud.db.Optio3DataSourceFactory;
import com.optio3.cloud.db.Optio3DbRateLimiter;
import com.optio3.cloud.logic.BackgroundActivityScheduler;
import com.optio3.cloud.logic.BackgroundActivityStatus;
import com.optio3.cloud.messagebus.MessageBusBroker;
import com.optio3.cloud.messagebus.channel.RpcChannel;
import com.optio3.cloud.messagebus.channel.RpcClient;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.model.scheduler.BackgroundActivityFilterPair;
import com.optio3.cloud.model.scheduler.BackgroundActivityFilterRequest;
import com.optio3.cloud.persistence.LogEntry;
import com.optio3.cloud.persistence.LogHolder;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordForBackgroundActivity;
import com.optio3.cloud.persistence.RecordForFixupProcessing;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.RecordWithMetadata;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.StreamHelperNextAction;
import com.optio3.concurrency.DebouncedAction;
import com.optio3.infra.NgrokHelper;
import com.optio3.infra.WellKnownSites;
import com.optio3.infra.directory.CredentialDirectory;
import com.optio3.infra.directory.RoleType;
import com.optio3.infra.directory.UserInfo;
import com.optio3.infra.docker.DockerImageDownloader;
import com.optio3.logging.ConsoleAppender;
import com.optio3.logging.ILogger;
import com.optio3.logging.ILoggerAppender;
import com.optio3.logging.Logger;
import com.optio3.logging.LoggerFactory;
import com.optio3.logging.LoggerResource;
import com.optio3.logging.Severity;
import com.optio3.util.ConfigVariables;
import com.optio3.util.Encryption;
import com.optio3.util.FileSystem;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.jetty.HttpsConnectorFactory;
import io.dropwizard.setup.Environment;
import org.apache.commons.lang3.StringUtils;

public class BuilderApplication extends AbstractApplicationWithDatabase<BuilderConfiguration>
{
    public static class FixupForMetadata extends RecordForFixupProcessing.Handler
    {
        public static class LegacyDeploymentHostDetails extends DeploymentHostDetails
        {
            public DeploymentCellularCharges charges;
        }

        @Override
        public RecordForFixupProcessing.Handler.Result process(Logger logger,
                                                               SessionHolder sessionHolder) throws
                                                                                            Exception
        {
            migrateCharges(sessionHolder);

            RecordWithMetadata.fixupFormat(logger, sessionHolder);

            return Result.Done;
        }

        private void migrateCharges(SessionHolder sessionHolder) throws
                                                                 Exception
        {
            QueryHelperWithCommonFields<Tuple, DeploymentHostRecord> jh = new QueryHelperWithCommonFields<>(sessionHolder.createHelper(DeploymentHostRecord.class), Tuple.class);

            QueryHelperWithCommonFields.stream(true, -1, jh, (rec) ->
            {
                boolean modified = false;

                var metadata = rec.getMetadata();

                var details = metadata.getObject("cellularDetails", LegacyDeploymentHostDetails.class);
                if (details != null)
                {
                    var charges = details.charges;
                    details.charges = null;
                    DeploymentHostRecord.WellKnownMetadata.cellularDetails.put(metadata, details);
                    DeploymentHostRecord.WellKnownMetadata.cellularCharges.put(metadata, charges);

                    DeploymentHostRecord.WellKnownMetadata.cellularDetails.get(metadata); // To verify roundtrip.

                    modified |= rec.setMetadata(metadata);
                }

                if (modified)
                {
                    rec.dontRefreshUpdatedOn();

                    return StreamHelperNextAction.Continue_Flush_Evict;
                }
                else
                {
                    return StreamHelperNextAction.Continue_Evict;
                }
            });

            sessionHolder.commitAndBeginNewTransaction();
        }
    }

    //--//

    static class LogEntryWithContext
    {
        ILogger  context;
        LogEntry entry;
    }

    //--//

    public enum EmailFlavor
    {
        Info,
        Warning,
        Alert,
        Provisioning
    }

    public static final Logger LoggerInstance = new Logger(BuilderApplication.class);

    private final Supplier<DockerImageDownloader> m_dockerImageDownloader = Suppliers.memoize(() -> new DockerImageDownloader(Duration.of(1, ChronoUnit.DAYS)));

    private BackgroundActivityScheduler<BackgroundActivityRecord, BackgroundActivityChunkRecord, HostRecord> m_bgScheduler;
    private DeploymentGlobalDescriptor.Spooler                                                               m_deploymentGlobalDescriptorSpooler;

    private List<Class<?>> m_entities;

    private NgrokHelper m_tunnelingHelper;

    //--//

    private final DebouncedAction<Void> m_logFlusher = new DebouncedAction<>(() ->
                                                                             {
                                                                                 flushLogEntries(false);

                                                                                 return AsyncRuntime.NullResult;
                                                                             });

    private final LinkedList<LogEntryWithContext> m_logEntries = new LinkedList<>();
    private       boolean                         m_logRedirectToDB;
    private       boolean                         m_logRedirectedToDB;

    //--//

    public static void main(String[] args) throws
                                           Exception
    {
        new BuilderApplication().run(args);
    }

    public BuilderApplication()
    {
        enableVariableSubstition = true;

        registerService(HostRemoter.class, () ->
        {
            BuilderConfiguration cfg = getServiceNonNull(BuilderConfiguration.class);
            return cfg.hostRemoter;
        });

        registerService(DockerImageDownloader.class, m_dockerImageDownloader::get);
    }

    @Override
    public String getName()
    {
        return "Optio3 Builder";
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
                BuilderConfiguration cfg = getServiceNonNull(BuilderConfiguration.class);

                try (SessionHolder holder = SessionHolder.createWithNewSessionWithoutTransaction(BuilderApplication.this, null, Optio3DbRateLimiter.Normal))
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

                BuilderConfiguration cfg = getServiceNonNull(BuilderConfiguration.class);

                try (SessionHolder holder = SessionHolder.createWithNewSessionWithoutTransaction(BuilderApplication.this, null, Optio3DbRateLimiter.Normal))
                {
                    UserRecord rec_user = cfg.userLogic.findUser(holder, principal, true);
                    if (rec_user != null)
                    {
                        for (RoleRecord rec_role : rec_user.getRoles())
                        {
                            res.add(rec_role.getName());
                        }
                    }
                }

                return res;
            }

            @Override
            public boolean authenticate(@NotNull CookiePrincipal principal,
                                        String password)
            {
                BuilderConfiguration cfg = getServiceNonNull(BuilderConfiguration.class);

                try (SessionHolder holder = SessionHolder.createWithNewSessionWithoutTransaction(BuilderApplication.this, null, Optio3DbRateLimiter.Normal))
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
                BuilderConfiguration cfg = getServiceNonNull(BuilderConfiguration.class);
                if (!cfg.noBackgroundProcessingDefragmentation)
                {
                    Optio3DataSourceFactory dataFactory = getDataSourceFactory(null);
                    dataFactory.queueDefragmentationAtBoot(m_entities);
                }
            }

            @Override
            protected HostRecord getWorker(SessionHolder sessionHolder)
            {
                return getCurrentHost(sessionHolder);
            }

            @Override
            protected ZonedDateTime findNextActivation(RecordHelper<BackgroundActivityRecord> helper)
            {
                return BackgroundActivityRecord.findNextActivation(helper, (HostRecord) null);
            }

            @Override
            protected TypedRecordIdentityList<BackgroundActivityRecord> listActivities(RecordHelper<BackgroundActivityRecord> helper,
                                                                                       HostRecord hostAffinity,
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
                return BuilderApplication.this.getService(serviceClass);
            }
        };
    }

    @Override
    protected void registerWithJersey(JerseyEnvironment jersey) throws
                                                                Exception
    {
        BuilderConfiguration cfg = getServiceNonNull(BuilderConfiguration.class);
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
                        publishLogEntry(context, timestamp, level, thread, selector, msg);
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
    }

    @Override
    protected void initializeDatabase(BuilderConfiguration cfg,
                                      Environment environment) throws
                                                               Exception
    {
        m_entities = enableHibernate(cfg, environment, null, cfg.getDataSourceFactory(), "com.optio3.cloud.builder.persistence.");
        if (m_entities != null)
        {
            DistributedLockingRecord.registerAsLockProvider(this, m_entities);
        }
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
        BuilderConfiguration cfg = getServiceNonNull(BuilderConfiguration.class);
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

        m_deploymentGlobalDescriptorSpooler = new DeploymentGlobalDescriptor.Spooler(this);

        //--//

        switch (cfg.deployerConnectionMode)
        {
            case TunnelToLocalhost:
            {
                HttpConnectorFactory connectorFactory = getApplicationConnector();

                m_tunnelingHelper = new NgrokHelper(Integer.toString(connectorFactory.getPort()));

                String prefix = (connectorFactory instanceof HttpsConnectorFactory) ? "wss://" : "ws://";
                String url    = m_tunnelingHelper.start();

                if (url != null)
                {
                    cfg.setCloudConnectionUrl(url.replace("tcp://", prefix));
                }
                else
                {
                    LoggerInstance.error("#########################################");
                    LoggerInstance.error("#########################################");
                    LoggerInstance.error("");
                    LoggerInstance.error("Not able to talk to NGrok!!!");
                    LoggerInstance.error("");
                    LoggerInstance.error("#########################################");
                    LoggerInstance.error("#########################################");
                }

                break;
            }

            case TunnelThroughNGrok:
            {
                HttpConnectorFactory connectorFactory = getApplicationConnector();

                String prefix = (connectorFactory instanceof HttpsConnectorFactory) ? "wss://" : "ws://";
                String url    = cfg.ngrokTunnelUrl;

                if (url != null)
                {
                    cfg.setCloudConnectionUrl(url.replace("tcp://", prefix));
                }
                else
                {
                    LoggerInstance.error("#########################################");
                    LoggerInstance.error("#########################################");
                    LoggerInstance.error("");
                    LoggerInstance.error("Not able to talk to NGrok!!!");
                    LoggerInstance.error("");
                    LoggerInstance.error("#########################################");
                    LoggerInstance.error("#########################################");
                }

                break;
            }

            case Localhost:
            {
                HttpConnectorFactory connectorFactory = getApplicationConnector();

                cfg.setCloudConnectionUrl(String.format("ws://localhost:%d", connectorFactory.getPort()));
                break;
            }

            default:
                break;
        }

        if (cfg.userLogic == null) // TestHarness overwrites this.
        {
            String file = cfg.credentialFile;
            if (file != null)
            {
                File file2 = new File(file);
                if (file2.exists())
                {
                    cfg.credentials = CredentialDirectory.load(file2);

                    if (cfg.developerSettings.useLocalhostAsNexus)
                    {
                        WellKnownSites.useLocalhostForNexus();

                        String oldSite = WellKnownSites.dockerRegistry(false);
                        String newSite = WellKnownSites.dockerRegistry(true);
                        cfg.credentials.remapSite(oldSite, newSite);
                    }

                    //--//

                    cfg.ldapRoot  = cfg.getCredentialForHost(WellKnownSites.ldapServer(), false, RoleType.Administrator);
                    cfg.userLogic = new BuilderUserLogicFromLdap(this, cfg.ldapRoot);

                    cfg.masterEncryptionKey = cfg.credentials.findFirstSecret(WellKnownSites.builderServer(), "masterKey").secretValue;
                }
            }
        }

        if (cfg.userLogic == null) // Maybe no credential file.
        {
            if (cfg.selfhost != null)
            {
                cfg.credentials = new CredentialDirectory();

                cfg.masterEncryptionKey = "masterKey";

                cfg.selfhost.user   = cfg.selfhost.emailAddress.split("@")[0];
                cfg.selfhost.ldapDn = String.format("uid=%s,ou=people,dc=optio3,dc=com", cfg.selfhost.user);

                cfg.ldapRoot = addCredentialForSelfhost(cfg.credentials.automationAccounts, WellKnownSites.ldapServer(), cfg.selfhost, RoleType.User, RoleType.Developer);
                addCredentialForSelfhost(cfg.credentials.automationAccounts, WellKnownSites.dockerRegistry(), cfg.selfhost, RoleType.Subscriber);

                cfg.userLogic = new BuilderUserLogicFromLdap(this, cfg.ldapRoot);

                try (SessionHolder holder = SessionHolder.createWithNewSessionWithTransaction(this, null, Optio3DbRateLimiter.System))
                {
                    UserRecord rec = new UserRecord();
                    rec.setEmailAddress("machine@demo.optio3.com");
                    rec.setPassword(holder, cfg.userLogic, "machinePwd");
                    for (RoleRecord role : cfg.userLogic.listRoles(holder))
                    {
                        if (role.getName()
                                .equals(WellKnownRole.Machine.getId()))
                        {
                            rec.getRoles()
                               .add(role);
                        }
                    }
                    holder.persistEntity(rec);

                    holder.commit();
                }
            }
        }

        //--//

        discoverResources("com.optio3.cloud.builder.api.");
        discoverWebSockets("/api", "com.optio3.cloud.builder.websocket.");
        discoverRemotableEndpoints("com.optio3.cloud.builder.remoting.impl.");
        discoverExtraModels("com.optio3.cloud.builder.model.");

        discoverMessageBusChannels("com.optio3.cloud.messagebus.channel.");

        serveAssets("/assets/website/dist", "/", "index.html", "BuilderUI", true, null);

        enableSwagger((config) ->
                      {
                          config.setVersion("1.0.0");
                          config.setTitle("Optio3 Builder APIs");
                          config.setDescription("APIs and Definitions for the Optio3 Builder product.");
                      }, (type, model) -> fixupRecordId(m_entities, type, model), "/api", "/api/v1", "com.optio3.cloud.builder.api");

        //--//

        m_deploymentGlobalDescriptorSpooler.initialize();

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

        if (cfg.enableMessageBusOverUDP)
        {
            broker.startUdpWorker(cfg.enableMessageBusOverUDPforIntel);
        }

        //--//

        if (cfg.userLogic != null)
        {
            cfg.userLogic.initialize();
        }

        cfg.hostRemoter = new HostRemoter(this, client);

        removeUntrackedDirectories(cfg);

        //--//

        if (cfg.startBackgroundProcessing)
        {
            if (cfg.developerSettings.developerMode)
            {
                //
                // When working with a backup, we could see a pending operation.
                // Make sure to kill all pending operations, or they might modify real cloud resources.
                //
                try (SessionHolder sessionHolder = SessionHolder.createWithNewSessionWithTransaction(this, null, Optio3DbRateLimiter.System))
                {
                    RecordHelper<BackgroundActivityRecord> helper = sessionHolder.createHelper(BackgroundActivityRecord.class);
                    for (BackgroundActivityRecord rec : helper.listAll())
                    {
                        if (!rec.getStatus()
                                .isDone())
                        {
                            rec.setResult(sessionHolder, new TimeoutException());
                        }
                    }

                    sessionHolder.commit();
                }
            }

            startScheduler("com.optio3.cloud.builder.");
        }

        if (cfg.loadDemoJobs)
        {
            loadDemoJobs(cfg);
        }
    }

    @Override
    protected void onServerStarted()
    {
        super.onServerStarted();

        BuilderConfiguration cfg = getServiceNonNull(BuilderConfiguration.class);
        if (cfg.credentials != null)
        {
            try
            {
                try (SessionHolder sessionHolder = SessionHolder.createWithNewSessionWithTransaction(this, null, Optio3DbRateLimiter.System))
                {
                    if (cfg.developerSettings.developerMode)
                    {
                        // Don't force refresh in developer mode.
                    }
                    else
                    {
                        TaskForRepositoryRefresh.scheduleTask(sessionHolder);
                    }

                    TaskForRegistryRefresh.scheduleTask(sessionHolder);

                    sessionHolder.commit();
                }
            }
            catch (Exception e)
            {
                LoggerInstance.error("Failed during initialization: %s", e);
                e.printStackTrace();
            }
        }
    }

    @Override
    public void cleanupOnShutdown(long timeout,
                                  TimeUnit unit) throws
                                                 Exception
    {
        stopScheduler(timeout, unit);

        if (m_tunnelingHelper != null)
        {
            m_tunnelingHelper.close();
            m_tunnelingHelper = null;
        }

        m_dockerImageDownloader.get()
                               .close();

        m_deploymentGlobalDescriptorSpooler.close();

        if (m_logRedirectedToDB)
        {
            flushLogEntries(true);
        }
    }

    //--//

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

    public HostRecord getCurrentHost(SessionHolder sessionHolder)
    {
        BuilderConfiguration cfg = getServiceNonNull(BuilderConfiguration.class);

        return sessionHolder.getEntity(HostRecord.class, cfg.host.sysId);
    }

    //--//

    public void sendEmailNotification(EmailFlavor flavor,
                                      String subject,
                                      ConfigVariables parameters)
    {
        BuilderConfiguration cfg = getServiceNonNull(BuilderConfiguration.class);

        String emailTo;

        switch (flavor)
        {
            case Info:
                emailTo = cfg.emailForInfo;
                break;

            case Warning:
                emailTo = cfg.emailForWarnings;
                break;

            case Alert:
                emailTo = cfg.emailForAlerts;
                break;

            case Provisioning:
                emailTo = cfg.emailForProvisioning;
                break;

            default:
                LoggerInstance.error("Unknown email flavor: %s", flavor);
                return;
        }

        sendEmailNotification(emailTo, subject, parameters);
    }

    public void sendEmailNotification(String emailTo,
                                      String subject,
                                      ConfigVariables parameters)
    {
        try
        {
            BuilderConfiguration cfg       = getServiceNonNull(BuilderConfiguration.class);
            String               emailText = parameters.convert();

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

            try (SessionHolder holder = SessionHolder.createWithNewSessionWithTransaction(this, null, Optio3DbRateLimiter.System))
            {
                EmailMessage message = new EmailMessage();
                message.from = TaskForNotification.newRecipient("no-reply-builder@optio3.io", null);
                message.to.add(TaskForNotification.newRecipient(emailTo, null));
                message.subject = subject;
                message.text    = emailText;

                TaskForNotification.scheduleTask(holder, null, message, null, null);

                holder.commit();
            }
        }
        catch (Throwable t)
        {
            LoggerInstance.error("Failed to send email: %s", t);
        }
    }

    //--//

    private void ensureCurrentHost(BuilderConfiguration cfg)
    {
        Host hostConfig = cfg.host;
        if (hostConfig == null)
        {
            throw new RuntimeException("Builder Host information not supplied.");
        }

        try (SessionHolder holder = SessionHolder.createWithNewSessionWithTransaction(this, null, Optio3DbRateLimiter.System))
        {
            HostRecord rec = holder.getEntityOrNull(HostRecord.class, hostConfig.sysId);
            if (rec == null)
            {
                if (hostConfig.ipAddress == null)
                {
                    try
                    {
                        InetAddress addr = InetAddress.getByName(hostConfig.domainName);
                        hostConfig.ipAddress = addr.getHostAddress();
                    }
                    catch (Exception e)
                    {
                        throw new RuntimeException(e);
                    }
                }

                rec = new HostRecord();
                rec.setSysId(hostConfig.sysId);
                rec.setDomainName(hostConfig.domainName);
                rec.setIpAddress(hostConfig.ipAddress);
                holder.persistEntity(rec);

                holder.commit();
            }
        }
    }

    private void removeUntrackedDirectories(BuilderConfiguration cfg) throws
                                                                      Exception
    {
        try (SessionHolder holder = SessionHolder.createWithNewSessionWithTransaction(this, null, Optio3DbRateLimiter.System))
        {
            RecordHelper<JobRecord> helper = holder.createHelper(JobRecord.class);
            for (JobRecord rec_job : helper.listAll())
            {
                switch (rec_job.getStatus())
                {
                    case EXECUTING:
                    case CANCELLING:
                        LoggerInstance.warn("Purging stale job: %s - %s", rec_job.getSysId(), rec_job.getName());
                        break;
                }

                try (ValidationResultsHolder validation = new ValidationResultsHolder(holder, null, false))
                {
                    for (JobStepRecord rec_step : Lists.newArrayList(rec_job.getSteps()))
                    {
                        rec_step.freeResources(cfg.hostRemoter, validation);
                    }
                }

                rec_job.releaseResources(cfg.hostRemoter, holder, false);

                rec_job.conditionallyChangeStatus(JobStatus.CANCELLING, JobStatus.CANCELLED);
                rec_job.conditionallyChangeStatus(JobStatus.EXECUTING, JobStatus.FAILED);
            }

            holder.commit();
        }

        try (SessionHolder holder = SessionHolder.createWithNewReadOnlySession(this, null, Optio3DbRateLimiter.System))
        {
            HostRecord rec_host = getCurrentHost(holder);

            Set<String> knownDirs = Sets.newHashSet();

            for (ManagedDirectoryRecord rec_dir : rec_host.getDirectories())
            {
                String fullPath = rec_dir.getPathAsString();
                knownDirs.add(fullPath);
            }

            Path root = Paths.get(cfg.managedDirectoriesRoot);
            root.toFile()
                .mkdirs();

            Files.list(root)
                 .forEach((p) ->
                          {
                              String fullPath = ManagedDirectoryRecord.getPathAsString(p);

                              if (knownDirs.contains(fullPath))
                              {
                                  return;
                              }

                              LoggerInstance.warn("Found untracked item: %s...", fullPath);

                              File f = p.toFile();
                              if (f.isFile())
                              {
                                  f.delete();
                              }
                              else
                              {
                                  FileSystem.deleteDirectory(p);
                              }

                              LoggerInstance.warn("Removed.");
                          });
        }
    }

    //--//

    private UserInfo addCredentialForSelfhost(Map<String, List<UserInfo>> automationAccounts,
                                              String site,
                                              UserInfo ui,
                                              RoleType... roles)
    {
        UserInfo ui2 = new UserInfo();
        ui2.user         = ui.user;
        ui2.emailAddress = ui.emailAddress;
        ui2.password     = ui.password;
        ui2.ldapDn       = ui.ldapDn;
        ui2.site         = site;
        ui2.roles.addAll(Arrays.asList(roles));

        automationAccounts.put(site, Lists.newArrayList(ui2));

        return ui2;
    }

    private void loadDemoJobs(BuilderConfiguration cfg) throws
                                                        Exception
    {
        RecordLocator<CustomerRecord> loc_cust = createOptio3Customer();

        createHubAndGateway(loc_cust, "demo", "https://demo.dev.optio3.io", true);
        createHubAndGateway(loc_cust, "demo-nightly", "https://demo-nightly.dev.optio3.io", true);
        createReporter(loc_cust, "reporter", "https://reporter.optio3.io");
        createReporter(loc_cust, "reporter-test", "https://reporter.dev.optio3.io");

        boolean localhost = cfg.deployerConnectionMode != BuilderConnectionMode.Production;
        if (localhost)
        {
            createHubAndGateway(loc_cust, "local-demo", "https://localhost.dev.optio3.io", true);
            createHubAndGateway(loc_cust, "local-demo-nightly", "https://localhost.dev.optio3.io:1443", true);
            createHubAndDatabase(loc_cust, "local-demo-nightly-maria", "https://localhost.dev.optio3.io:1443", true);
            createReporter(loc_cust, "local-reporter", "https://localhost.dev.optio3.io:3443");
        }

        RecordLocator<RepositoryRecord> loc_repo = createCoreRepo();
        loadBuild_HelloWorld(loc_repo);
        loadBuild_CoreTest(loc_repo);
        loadBuild_BuildHub(loc_repo);
        loadBuild_BuildGateway(loc_repo);
        loadBuild_BuildBuilder(loc_repo);
        loadBuild_BuildDeployer(loc_repo);
        loadBuild_BuildWaypoint(loc_repo);
        loadBuild_BuildProvisioner(loc_repo);
        loadBuild_BuildReporter(loc_repo);
    }

    private void reportNewService(CustomerServiceRecord rec_svc) throws
                                                                 Exception
    {
        LoggerInstance.info("NewService '%s': %s => AccessKey = %s", rec_svc.getName(), rec_svc.getSysId(), rec_svc.getAccessKey(m_configuration));
    }

    private RecordLocator<CustomerRecord> createOptio3Customer()
    {
        try (SessionHolder holder = SessionHolder.createWithNewSessionWithoutTransaction(this, null, Optio3DbRateLimiter.System))
        {
            final String name = "Optio3, Inc.";

            CustomerRecord rec_cust = CustomerRecord.findByName(holder.createHelper(CustomerRecord.class), name);
            if (rec_cust == null)
            {
                holder.beginTransaction();

                //--//

                rec_cust = new CustomerRecord();

                rec_cust.setName(name);

                holder.persistEntity(rec_cust);

                holder.commit();
            }

            return holder.createLocator(rec_cust);
        }
    }

    private RecordLocator<CustomerServiceRecord> createHubAndGateway(RecordLocator<CustomerRecord> cust_loc,
                                                                     String name,
                                                                     String url,
                                                                     boolean useDemoData) throws
                                                                                          Exception
    {
        try (SessionHolder holder = SessionHolder.createWithNewSessionWithoutTransaction(this, null, Optio3DbRateLimiter.System))
        {
            CustomerRecord rec_cust = holder.fromLocator(cust_loc);

            for (CustomerServiceRecord rec_svc : rec_cust.getServices())
            {
                if (StringUtils.equals(rec_svc.getName(), name))
                {
                    return holder.createLocator(rec_svc);
                }
            }

            //--//

            holder.beginTransaction();

            BuilderConfiguration cfg = getServiceNonNull(BuilderConfiguration.class);

            CustomerServiceRecord rec_svc = CustomerServiceRecord.newInstance(rec_cust);
            rec_svc.setName(name);
            rec_svc.setUrl(url);
            rec_svc.setUseDemoData(useDemoData);

            rec_svc.setPurposes(Sets.newHashSet(DeploymentRole.hub, DeploymentRole.gateway));

            rec_svc.setMasterKey(cfg.encrypt(Encryption.generateRandomKeyAsBase64()));

            EmbeddedDatabaseConfiguration dbConfig = new EmbeddedDatabaseConfiguration();
            dbConfig.setMode(DatabaseMode.H2OnDisk);
            dbConfig.setDatabaseName("hub_db");
            dbConfig.setDatabaseUser("sa");
            dbConfig.setDatabasePassword(cfg.encrypt("sa"));

            rec_svc.setDbConfiguration(dbConfig);

            holder.persistEntity(rec_svc);
            reportNewService(rec_svc);

            holder.commit();

            //--//

            return holder.createLocator(rec_svc);
        }
    }

    private RecordLocator<CustomerServiceRecord> createHubAndDatabase(RecordLocator<CustomerRecord> cust_loc,
                                                                      String name,
                                                                      String url,
                                                                      boolean useDemoData) throws
                                                                                           Exception
    {
        try (SessionHolder holder = SessionHolder.createWithNewSessionWithoutTransaction(this, null, Optio3DbRateLimiter.System))
        {
            CustomerRecord rec_cust = holder.fromLocator(cust_loc);

            for (CustomerServiceRecord rec_svc : rec_cust.getServices())
            {
                if (StringUtils.equals(rec_svc.getName(), name))
                {
                    return holder.createLocator(rec_svc);
                }
            }

            //--//

            holder.beginTransaction();

            BuilderConfiguration cfg = getServiceNonNull(BuilderConfiguration.class);

            CustomerServiceRecord rec_svc = CustomerServiceRecord.newInstance(rec_cust);
            rec_svc.setName(name);
            rec_svc.setUrl(url);
            rec_svc.setUseDemoData(useDemoData);

            rec_svc.setPurposes(Sets.newHashSet(DeploymentRole.hub, DeploymentRole.database, DeploymentRole.gateway));

            rec_svc.setMasterKey(cfg.encrypt(Encryption.generateRandomKeyAsBase64()));

            EmbeddedDatabaseConfiguration dbConfig = new EmbeddedDatabaseConfiguration();
            dbConfig.setMode(DatabaseMode.MariaDB);
            dbConfig.setDatabaseName("hub_db");
            dbConfig.setDatabaseUser("root");
            dbConfig.setDatabasePassword(cfg.encrypt("test"));
            dbConfig.setServer("database_host:3306");

            rec_svc.setDbConfiguration(dbConfig);

            holder.persistEntity(rec_svc);
            reportNewService(rec_svc);

            holder.commit();

            //--//

            return holder.createLocator(rec_svc);
        }
    }

    private RecordLocator<CustomerServiceRecord> createReporter(RecordLocator<CustomerRecord> cust_loc,
                                                                String name,
                                                                String url)
    {
        try (SessionHolder holder = SessionHolder.createWithNewSessionWithoutTransaction(this, null, Optio3DbRateLimiter.System))
        {
            CustomerRecord rec_cust = holder.fromLocator(cust_loc);

            for (CustomerServiceRecord rec_svc : rec_cust.getServices())
            {
                if (StringUtils.equals(rec_svc.getName(), name))
                {
                    return holder.createLocator(rec_svc);
                }
            }

            //--//

            holder.beginTransaction();

            BuilderConfiguration cfg = getServiceNonNull(BuilderConfiguration.class);

            CustomerServiceRecord rec_svc = CustomerServiceRecord.newInstance(rec_cust);
            rec_svc.setName(name);
            rec_svc.setUrl(url);

            rec_svc.setPurposes(Sets.newHashSet(DeploymentRole.reporter));

            EmbeddedDatabaseConfiguration dbConfig = new EmbeddedDatabaseConfiguration();
            dbConfig.setMode(DatabaseMode.None);

            rec_svc.setDbConfiguration(dbConfig);

            holder.persistEntity(rec_svc);

            holder.commit();

            //--//

            return holder.createLocator(rec_svc);
        }
    }

    private RecordLocator<RepositoryRecord> createCoreRepo()
    {
        try (SessionHolder holder = SessionHolder.createWithNewSessionWithoutTransaction(this, null, Optio3DbRateLimiter.System))
        {
            final String name = "core";

            RepositoryRecord rec_repo = RepositoryRecord.findByName(holder.createHelper(RepositoryRecord.class), name);
            if (rec_repo == null)
            {
                holder.beginTransaction();

                rec_repo = new RepositoryRecord();
                rec_repo.setName(name);

                BuilderConfiguration cfg = getServiceNonNull(BuilderConfiguration.class);
                if (cfg.developerSettings.sourceRepo != null)
                {
                    rec_repo.setGitUrl(cfg.developerSettings.sourceRepo);
                }
                else
                {
                    rec_repo.setGitUrl("https://github.com/optio3/core.git");
                }

                holder.persistEntity(rec_repo);

                holder.commit();
            }

            return holder.createLocator(rec_repo);
        }
    }

    private void loadBuild_HelloWorld(RecordLocator<RepositoryRecord> rec_loc)
    {
        try (SessionHolder holder = SessionHolder.createWithNewSessionWithTransaction(this, null, Optio3DbRateLimiter.System))
        {
            final String name = "zzz - Hello World";

            JobDefinitionRecord rec_jobDef = JobDefinitionRecord.findByName(holder.createHelper(JobDefinitionRecord.class), name);
            if (rec_jobDef == null)
            {
                rec_jobDef = new JobDefinitionRecord();
                rec_jobDef.setIdPrefix("hello_world");
                rec_jobDef.setName(name);
                rec_jobDef.setTotalTimeout(5 * 60);
                holder.persistEntity(rec_jobDef);
            }

            BuildStepHelper helper = new BuildStepHelper(holder.createHelper(JobDefinitionStepRecord.class), rec_jobDef);

            RepositoryRecord rec_repo = holder.fromLocator(rec_loc);
            helper.createRepoStep(rec_repo, "repo1");

            helper.createMavenStep("maven");

            helper.createBuildStep("Build1", "Build", "${repo1.dir}", "${maven.dir}", "echo Hello World", "${repo1.dir}");

            helper.createSshStep("Deploy1", "Deploy", "ls -al /optio3*", "ec2-user@amazon.com", "demo-nightly.dev.optio3.io");

            //--//

            holder.commit();
        }
    }

    private void loadBuild_CoreTest(RecordLocator<RepositoryRecord> rec_loc)
    {
        try (SessionHolder holder = SessionHolder.createWithNewSessionWithTransaction(this, null, Optio3DbRateLimiter.System))
        {
            final String name = "zzz - Test";

            JobDefinitionRecord rec_jobDef = JobDefinitionRecord.findByName(holder.createHelper(JobDefinitionRecord.class), name);
            if (rec_jobDef == null)
            {
                rec_jobDef = new JobDefinitionRecord();
                rec_jobDef.setIdPrefix("test");
                rec_jobDef.setName(name);
                rec_jobDef.setTotalTimeout(30 * 60);
                holder.persistEntity(rec_jobDef);
            }

            BuildStepHelper helper = new BuildStepHelper(holder.createHelper(JobDefinitionStepRecord.class), rec_jobDef);

            RepositoryRecord rec_repo = holder.fromLocator(rec_loc);
            helper.createRepoStep(rec_repo, "repo1");

            helper.createMavenStep("maven");

            helper.createBuildStep("projects",
                                   "Build projects",
                                   "${repo1.dir}",
                                   "${maven.dir}",
                                   "mvn package -Pcollect-dependencies -Dmaven.test.skip=true --also-make --projects cli/explorer-bacnet/pom.xml",
                                   "${repo1.dir}");

//                "mvn package -Dmaven.test.skip=true -Pasyncawait --also-make --projects product/hub/pom.xml"
//                "mvn package -Pasyncawait --also-make --projects product/hub/pom.xml"
//                mvn test --also-make --projects common-test/pom.xml"
//                mvn test"

            helper.createImageBuildStep("imageProtocol1", "Build Modbus Image", "${repo1.dir}/cli/explorer-modbus", "Dockerfile", DockerImageArchitecture.X86, DeploymentRole.test);

            helper.createImagePushStep("imagePushHub1", "Push Test Image", "${imageProtocol1.imageTemporaryTag}", "builder-test");

            //--//

            holder.commit();
        }
    }

    private void loadBuild_BuildHub(RecordLocator<RepositoryRecord> rec_loc)
    {
        try (SessionHolder holder = SessionHolder.createWithNewSessionWithTransaction(this, null, Optio3DbRateLimiter.System))
        {
            final String name = "Hub CI pipeline";

            JobDefinitionRecord rec_jobDef = JobDefinitionRecord.findByName(holder.createHelper(JobDefinitionRecord.class), name);
            if (rec_jobDef == null)
            {
                rec_jobDef = new JobDefinitionRecord();
                rec_jobDef.setIdPrefix("hub");
                rec_jobDef.setName(name);
                rec_jobDef.setTotalTimeout(30 * 60);
                holder.persistEntity(rec_jobDef);
            }

            BuildStepHelper helper = new BuildStepHelper(holder.createHelper(JobDefinitionStepRecord.class), rec_jobDef);

            RepositoryRecord rec_repo = holder.fromLocator(rec_loc);
            helper.createRepoStep(rec_repo, "repo1");

            helper.createMavenStep("maven");

            helper.createToolingStep("${repo1.dir}", "${maven.dir}");

            {
                StringBuilder sb = new StringBuilder();
                sb.append("mvn package");

                BuilderConfiguration cfg = getServiceNonNull(BuilderConfiguration.class);
                if (cfg.developerSettings.useLocalMaven)
                {
                    sb.append(" -Dmaven.test.skip=true"); // Skip tests on a local build.
                }

                sb.append(" -Pasyncawait --also-make --projects product/hub/pom.xml --projects product/gateway/pom.xml");

                JobDefinitionStepRecordForDockerRun rec_stepDef = helper.createBuildStep("projects", "Build projects", "${repo1.dir}", "${maven.dir}", sb.toString(), "${repo1.dir}");
                if (false) // Disabling for now, until we sort out Azure access.
                {
                    rec_stepDef.publishToCdn("product/hub/src/main/resources/assets/website/dist", "website");
                    rec_stepDef.publishToCdn("product/hub/src/main/resources/assets/wdc/dist", "wdc");
                }
            }

            helper.createImageBuildStep("imageHubIntel", "Build Hub Intel Image", "${repo1.dir}/product/hub", "Dockerfile", DockerImageArchitecture.X86, DeploymentRole.hub);
            helper.createImageBuildStep("imageHubARM64", "Build Hub ARM64 Image", "${repo1.dir}/product/hub", "Dockerfile.arm64", DockerImageArchitecture.ARM64v8, DeploymentRole.hub);

            helper.createImageBuildStep("imageGatewayIntel", "Build Gateway Intel Image", "${repo1.dir}/product/gateway", "Dockerfile", DockerImageArchitecture.X86, DeploymentRole.gateway);
            helper.createImageBuildStep("imageGatewayArm64", "Build Gateway ARM64 Image", "${repo1.dir}/product/gateway", "Dockerfile.arm64", DockerImageArchitecture.ARM64v8, DeploymentRole.gateway);
            helper.createImageBuildStep("imageGatewayArmV7", "Build Gateway ARMv7 Image", "${repo1.dir}/product/gateway", "Dockerfile.armv7", DockerImageArchitecture.ARMv7, DeploymentRole.gateway);

            //--//

            helper.createImagePushStep("imagePushHubIntel", "Push Hub Intel Image", "${imageHubIntel.imageTemporaryTag}", "optio3-hub");
            helper.createImagePushStep("imagePushHubARM64", "Push Hub ARM64 Image", "${imageHubARM64.imageTemporaryTag}", "optio3-hub-arm64");

            helper.createImagePushStep("imagePushGatewayIntel", "Push Gateway Intel Image", "${imageGatewayIntel.imageTemporaryTag}", "optio3-gateway");
            helper.createImagePushStep("imagePushGatewayArm64", "Push Gateway ARM64 Image", "${imageGatewayArm64.imageTemporaryTag}", "optio3-gateway-arm64");
            helper.createImagePushStep("imagePushGatewayArmV7", "Push Gateway ARMv7 Image", "${imageGatewayArmV7.imageTemporaryTag}", "optio3-gateway-armv7");

            //--//

            holder.commit();
        }
    }

    private void loadBuild_BuildBuilder(RecordLocator<RepositoryRecord> rec_loc)
    {
        try (SessionHolder holder = SessionHolder.createWithNewSessionWithTransaction(this, null, Optio3DbRateLimiter.System))
        {
            final String name = "Builder CI pipeline";

            JobDefinitionRecord rec_jobDef = JobDefinitionRecord.findByName(holder.createHelper(JobDefinitionRecord.class), name);
            if (rec_jobDef == null)
            {
                rec_jobDef = new JobDefinitionRecord();
                rec_jobDef.setIdPrefix("builder");
                rec_jobDef.setName(name);
                rec_jobDef.setTotalTimeout(30 * 60);
                holder.persistEntity(rec_jobDef);
            }

            BuildStepHelper helper = new BuildStepHelper(holder.createHelper(JobDefinitionStepRecord.class), rec_jobDef);

            RepositoryRecord rec_repo = holder.fromLocator(rec_loc);
            helper.createRepoStep(rec_repo, "repo1");

            helper.createMavenStep("maven");

            helper.createToolingStep("${repo1.dir}", "${maven.dir}");

            {
                StringBuilder sb = new StringBuilder();
                sb.append("mvn package");

                BuilderConfiguration cfg = getServiceNonNull(BuilderConfiguration.class);
                if (cfg.developerSettings.useLocalMaven)
                {
                    sb.append(" -Dmaven.test.skip=true"); // Skip tests on a local build.
                }

                sb.append(" -Pasyncawait --also-make --projects product/builder/pom.xml");

                helper.createBuildStep("projects", "Build projects", "${repo1.dir}", "${maven.dir}", sb.toString(), "${repo1.dir}");
            }

            helper.createImageBuildStep("imageBuilder", "Build Builder Image", "${repo1.dir}/product/builder", "Dockerfile", DockerImageArchitecture.X86, DeploymentRole.builder);

            helper.createImagePushStep("imagePushBuilder", "Push Builder Image", "${imageBuilder.imageTemporaryTag}", "optio3-builder");

            //--//

            holder.commit();
        }
    }

    private void loadBuild_BuildDeployer(RecordLocator<RepositoryRecord> rec_loc)
    {
        try (SessionHolder holder = SessionHolder.createWithNewSessionWithTransaction(this, null, Optio3DbRateLimiter.System))
        {
            final String name = "Deployer CI pipeline";

            JobDefinitionRecord rec_jobDef = JobDefinitionRecord.findByName(holder.createHelper(JobDefinitionRecord.class), name);
            if (rec_jobDef == null)
            {
                rec_jobDef = new JobDefinitionRecord();
                rec_jobDef.setIdPrefix("deployer");
                rec_jobDef.setName(name);
                rec_jobDef.setTotalTimeout(30 * 60);
                holder.persistEntity(rec_jobDef);
            }

            BuildStepHelper helper = new BuildStepHelper(holder.createHelper(JobDefinitionStepRecord.class), rec_jobDef);

            RepositoryRecord rec_repo = holder.fromLocator(rec_loc);
            helper.createRepoStep(rec_repo, "repo1");

            helper.createMavenStep("maven");

            helper.createToolingStep("${repo1.dir}", "${maven.dir}");

            {
                StringBuilder sb = new StringBuilder();
                sb.append("mvn package");

                BuilderConfiguration cfg = getServiceNonNull(BuilderConfiguration.class);
                if (cfg.developerSettings.useLocalMaven)
                {
                    sb.append(" -Dmaven.test.skip=true"); // Skip tests on a local build.
                }

                sb.append(" -Pasyncawait --also-make --projects product/deployer/pom.xml");

                JobDefinitionStepRecordForDockerRun rec_stepDef = helper.createBuildStep("projects", "Build projects", "${repo1.dir}", "${maven.dir}", sb.toString(), "${repo1.dir}");

                rec_stepDef.setEnvironmentVariable("OPTIO3_SKIP_NPM_BUILD", "1");
            }

            helper.createImageBuildStep("imageIntel", "Build Deployer Intel Image", "${repo1.dir}/product/deployer", "Dockerfile", DockerImageArchitecture.X86, DeploymentRole.deployer);
            helper.createImageBuildStep("imageArm64", "Build Deployer ARM64 Image", "${repo1.dir}/product/deployer", "Dockerfile.arm64", DockerImageArchitecture.ARM64v8, DeploymentRole.deployer);
            helper.createImageBuildStep("imageArmV7", "Build Deployer ARMv7 Image", "${repo1.dir}/product/deployer", "Dockerfile.armv7", DockerImageArchitecture.ARMv7, DeploymentRole.deployer);

            helper.createImagePushStep("imagePushIntel", "Push Deployer Intel Image", "${imageIntel.imageTemporaryTag}", "optio3-deployer");
            helper.createImagePushStep("imagePushDeployerArm64", "Push Deployer ARM64 Image", "${imageArm64.imageTemporaryTag}", "optio3-deployer-arm64");
            helper.createImagePushStep("imagePushDeployerArmV7", "Push Deployer ARMv7 Image", "${imageArmV7.imageTemporaryTag}", "optio3-deployer-armv7");

            //--//

            holder.commit();
        }
    }

    private void loadBuild_BuildWaypoint(RecordLocator<RepositoryRecord> rec_loc)
    {
        try (SessionHolder holder = SessionHolder.createWithNewSessionWithTransaction(this, null, Optio3DbRateLimiter.System))
        {
            final String name = "Waypoint CI pipeline";

            JobDefinitionRecord rec_jobDef = JobDefinitionRecord.findByName(holder.createHelper(JobDefinitionRecord.class), name);
            if (rec_jobDef == null)
            {
                rec_jobDef = new JobDefinitionRecord();
                rec_jobDef.setIdPrefix("waypoint");
                rec_jobDef.setName(name);
                rec_jobDef.setTotalTimeout(30 * 60);
                holder.persistEntity(rec_jobDef);
            }

            BuildStepHelper helper = new BuildStepHelper(holder.createHelper(JobDefinitionStepRecord.class), rec_jobDef);

            RepositoryRecord rec_repo = holder.fromLocator(rec_loc);
            helper.createRepoStep(rec_repo, "repo1");

            helper.createMavenStep("maven");

            helper.createToolingStep("${repo1.dir}", "${maven.dir}");

            {
                StringBuilder sb = new StringBuilder();
                sb.append("mvn package -Dmaven.test.skip=true"); // Skip tests on waypoint.

                sb.append(" -Pasyncawait --also-make --projects product/waypoint/pom.xml");

                helper.createBuildStep("projects", "Build projects", "${repo1.dir}", "${maven.dir}", sb.toString(), "${repo1.dir}");
            }

            helper.createImageBuildStep("imageIntel", "Build Waypoint Intel Image", "${repo1.dir}/product/waypoint", "Dockerfile", DockerImageArchitecture.X86, DeploymentRole.waypoint);
            helper.createImageBuildStep("imageArmV7", "Build Waypoint ARMv7 Image", "${repo1.dir}/product/waypoint", "Dockerfile.armv7", DockerImageArchitecture.ARMv7, DeploymentRole.waypoint);

            helper.createImagePushStep("imagePushWaypointIntel", "Push Waypoint Intel Image", "${imageIntel.imageTemporaryTag}", "optio3-waypoint");
            helper.createImagePushStep("imagePushWaypointArmV7", "Push Waypoint ARMv7 Image", "${imageArmV7.imageTemporaryTag}", "optio3-waypoint-armv7");

            //--//

            holder.commit();
        }
    }

    private void loadBuild_BuildProvisioner(RecordLocator<RepositoryRecord> rec_loc)
    {
        try (SessionHolder holder = SessionHolder.createWithNewSessionWithTransaction(this, null, Optio3DbRateLimiter.System))
        {
            final String name = "Provisioner CI pipeline";

            JobDefinitionRecord rec_jobDef = JobDefinitionRecord.findByName(holder.createHelper(JobDefinitionRecord.class), name);
            if (rec_jobDef == null)
            {
                rec_jobDef = new JobDefinitionRecord();
                rec_jobDef.setIdPrefix("provisioner");
                rec_jobDef.setName(name);
                rec_jobDef.setTotalTimeout(30 * 60);
                holder.persistEntity(rec_jobDef);
            }

            BuildStepHelper helper = new BuildStepHelper(holder.createHelper(JobDefinitionStepRecord.class), rec_jobDef);

            RepositoryRecord rec_repo = holder.fromLocator(rec_loc);
            helper.createRepoStep(rec_repo, "repo1");

            helper.createMavenStep("maven");

            helper.createToolingStep("${repo1.dir}", "${maven.dir}");

            {
                StringBuilder sb = new StringBuilder();
                sb.append("mvn package -Dmaven.test.skip=true"); // Skip tests on provisioner.

                sb.append(" -Pasyncawait --also-make --projects product/provisioner/pom.xml");

                helper.createBuildStep("projects", "Build projects", "${repo1.dir}", "${maven.dir}", sb.toString(), "${repo1.dir}");
            }

            helper.createImageBuildStep("imageArmV7",
                                        "Build Provisioner ARMv7 Image",
                                        "${repo1.dir}/product/provisioner",
                                        "Dockerfile.armv7",
                                        DockerImageArchitecture.ARMv7,
                                        DeploymentRole.provisioner);

            helper.createImagePushStep("imagePushProvisionerArmV7", "Push Provisioner ARMv7 Image", "${imageArmV7.imageTemporaryTag}", "optio3-provisioner-armv7");

            //--//

            holder.commit();
        }
    }

    private void loadBuild_BuildReporter(RecordLocator<RepositoryRecord> rec_loc)
    {
        try (SessionHolder holder = SessionHolder.createWithNewSessionWithTransaction(this, null, Optio3DbRateLimiter.System))
        {
            final String name = "Reporter CI pipeline";

            JobDefinitionRecord rec_jobDef = JobDefinitionRecord.findByName(holder.createHelper(JobDefinitionRecord.class), name);
            if (rec_jobDef == null)
            {
                rec_jobDef = new JobDefinitionRecord();
                rec_jobDef.setIdPrefix("reporter");
                rec_jobDef.setName(name);
                rec_jobDef.setTotalTimeout(30 * 60);
                holder.persistEntity(rec_jobDef);
            }

            BuildStepHelper helper = new BuildStepHelper(holder.createHelper(JobDefinitionStepRecord.class), rec_jobDef);

            RepositoryRecord rec_repo = holder.fromLocator(rec_loc);
            helper.createRepoStep(rec_repo, "repo1");

            helper.createImageBuildStep("image", "Build Reporter Intel Image", "${repo1.dir}/web/reporter", "Dockerfile", DockerImageArchitecture.X86, DeploymentRole.reporter);

            helper.createImagePushStep("imagePush", "Push Reporter Intel Image", "${image.imageTemporaryTag}", "optio3-reporter");

            //--//

            holder.commit();
        }
    }

    private void loadBuild_BuildGateway(RecordLocator<RepositoryRecord> rec_loc)
    {
        try (SessionHolder holder = SessionHolder.createWithNewSessionWithTransaction(this, null, Optio3DbRateLimiter.System))
        {
            final String name = "Gateway CI pipeline";

            JobDefinitionRecord rec_jobDef = JobDefinitionRecord.findByName(holder.createHelper(JobDefinitionRecord.class), name);
            if (rec_jobDef == null)
            {
                rec_jobDef = new JobDefinitionRecord();
                rec_jobDef.setIdPrefix("gateway");
                rec_jobDef.setName(name);
                rec_jobDef.setTotalTimeout(30 * 60);
                holder.persistEntity(rec_jobDef);
            }

            BuildStepHelper helper = new BuildStepHelper(holder.createHelper(JobDefinitionStepRecord.class), rec_jobDef);

            RepositoryRecord rec_repo = holder.fromLocator(rec_loc);
            helper.createRepoStep(rec_repo, "repo1");

            helper.createMavenStep("maven");

            helper.createToolingStep("${repo1.dir}", "${maven.dir}");

            {
                StringBuilder sb = new StringBuilder();
                sb.append("mvn package");

                BuilderConfiguration cfg = getServiceNonNull(BuilderConfiguration.class);
                if (cfg.developerSettings.useLocalMaven)
                {
                    sb.append(" -Dmaven.test.skip=true"); // Skip tests on a local build.
                }

                sb.append(" -Pasyncawait --also-make --projects product/gateway/pom.xml");

                JobDefinitionStepRecordForDockerRun rec_stepDef = helper.createBuildStep("projects", "Build projects", "${repo1.dir}", "${maven.dir}", sb.toString(), "${repo1.dir}");

                rec_stepDef.setEnvironmentVariable("OPTIO3_SKIP_NPM_BUILD", "1");
            }

            helper.createImageBuildStep("imageIntel", "Build Gateway Intel Image", "${repo1.dir}/product/gateway", "Dockerfile", DockerImageArchitecture.X86, DeploymentRole.gateway);
            helper.createImageBuildStep("imageArm64", "Build Gateway ARM64 Image", "${repo1.dir}/product/gateway", "Dockerfile.arm64", DockerImageArchitecture.ARM64v8, DeploymentRole.gateway);
            helper.createImageBuildStep("imageArmV7", "Build Gateway ARMv7 Image", "${repo1.dir}/product/gateway", "Dockerfile.armv7", DockerImageArchitecture.ARMv7, DeploymentRole.gateway);

            helper.createImagePushStep("imagePushIntel", "Push Gateway Intel Image", "${imageIntel.imageTemporaryTag}", "optio3-gateway");
            helper.createImagePushStep("imagePushArm64", "Push Gateway ARM64 Image", "${imageArm64.imageTemporaryTag}", "optio3-gateway-arm64");
            helper.createImagePushStep("imagePushArmV7", "Push Gateway ARMv7 Image", "${imageArmV7.imageTemporaryTag}", "optio3-gateway-armv7");

            //--//

            holder.commit();
        }
    }

    //--//

    static class BuildStepHelper
    {
        private final RecordHelper<JobDefinitionStepRecord> helper;
        private final JobDefinitionRecord                   rec_jobDef;
        private       int                                   pos;

        private BuildStepHelper(RecordHelper<JobDefinitionStepRecord> helper,
                                JobDefinitionRecord rec_jobDef)
        {
            this.helper     = helper;
            this.rec_jobDef = rec_jobDef;

            // Remove all the steps, we are going to recreate them.
            for (JobDefinitionStepRecord rec_oldStep : Lists.newArrayList(rec_jobDef.getSteps()))
            {
                helper.delete(rec_oldStep);
            }
        }

        private void createRepoStep(RepositoryRecord rec_repo,
                                    String stepId)
        {
            JobDefinitionStepRecordForGit rec_stepDef = new JobDefinitionStepRecordForGit(rec_jobDef);
            rec_stepDef.setBuildId(stepId);
            rec_stepDef.setName("Clone repository");
            rec_stepDef.setPosition(pos++);
            rec_stepDef.setRepo(rec_repo);
            rec_stepDef.setDirectory("/sources/core");

            helper.persist(rec_stepDef);
        }

        private void createMavenStep(String stepId)
        {
            JobDefinitionStepRecordForMaven rec_stepDef = new JobDefinitionStepRecordForMaven(rec_jobDef);
            rec_stepDef.setBuildId(stepId);
            rec_stepDef.setName("Configure local Maven repository");
            rec_stepDef.setPosition(pos++);
            rec_stepDef.setPullFrom("maven-public");
            rec_stepDef.setDirectory("/var/maven/localrepository");

            helper.persist(rec_stepDef);
        }

        private void createToolingStep(String repoDir,
                                       String mavenDir)
        {
            createBuildStep("tools", "Build tools", repoDir, mavenDir, "scripts/o3 tooling mvn-plugin", "${repo1.dir}");
        }

        private JobDefinitionStepRecordForDockerRun createBuildStep(String stepId,
                                                                    String stepName,
                                                                    String repoDir,
                                                                    String mavenDir,
                                                                    String commandLine,
                                                                    String workingDir)
        {
            JobDefinitionStepRecordForDockerRun rec_stepDef = new JobDefinitionStepRecordForDockerRun(rec_jobDef);
            rec_stepDef.setBuildId(stepId);
            rec_stepDef.setName(stepName);
            rec_stepDef.setPosition(pos++);

            rec_stepDef.setImage("optio3-maven:3.6.3-jdk-11");

            rec_stepDef.addBinding(mavenDir);
            rec_stepDef.addBinding(repoDir);

            rec_stepDef.setCommandLine(commandLine);

            rec_stepDef.setWorkingDirectory(workingDir);

            helper.persist(rec_stepDef);

            return rec_stepDef;
        }

        private void createImageBuildStep(String stepId,
                                          String stepName,
                                          String buildDir,
                                          String dockerFile,
                                          DockerImageArchitecture arch,
                                          DeploymentRole serviceTag)
        {
            JobDefinitionStepRecordForDockerBuild rec_stepDef = new JobDefinitionStepRecordForDockerBuild(rec_jobDef);
            rec_stepDef.setBuildId(stepId);
            rec_stepDef.setName(stepName);
            rec_stepDef.setPosition(pos++);

            rec_stepDef.setSourcePath(buildDir);
            rec_stepDef.setDockerFile(dockerFile);
            rec_stepDef.setArchitecture(arch);
            rec_stepDef.setTargetService(serviceTag);

            helper.persist(rec_stepDef);
        }

        private void createImagePushStep(String stepId,
                                         String stepName,
                                         String imageSource,
                                         String imageTag)
        {
            JobDefinitionStepRecordForDockerPush rec_stepDef = new JobDefinitionStepRecordForDockerPush(rec_jobDef);
            rec_stepDef.setBuildId(stepId);
            rec_stepDef.setName(stepName);
            rec_stepDef.setPosition(pos++);

            rec_stepDef.setSourceImage(imageSource);
            rec_stepDef.setImageTag(imageTag);

            helper.persist(rec_stepDef);
        }

        private void createSshStep(String stepId,
                                   String stepName,
                                   String commandLine,
                                   String credentials,
                                   String targetHost)
        {
            JobDefinitionStepRecordForSshCommand rec_stepDef = new JobDefinitionStepRecordForSshCommand(rec_jobDef);
            rec_stepDef.setBuildId(stepId);
            rec_stepDef.setName(stepName);
            rec_stepDef.setPosition(pos++);

            rec_stepDef.setCommandLine(commandLine);
            rec_stepDef.setCredentials(credentials);
            rec_stepDef.setTargetHost(targetHost);

            helper.persist(rec_stepDef);
        }
    }

    //--//

    private void publishLogEntry(ILogger context,
                                 ZonedDateTime timestamp,
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

        LogEntryWithContext enWithContext = new LogEntryWithContext();
        enWithContext.context = context;
        enWithContext.entry   = en;

        synchronized (m_logEntries)
        {
            final int maxPendingEntries = 50_000;

            if (m_logEntries.size() > maxPendingEntries)
            {
                // Avoid running out of memory...
                m_logEntries.removeFirst();
            }

            m_logEntries.add(enWithContext);

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

                for (LogEntryWithContext entry : m_logEntries)
                {
                    logger.append(entry.context, entry.entry.timestamp, entry.entry.level, entry.entry.thread, entry.entry.selector, entry.entry.line);
                }
            }
        }

        BuilderConfiguration cfg = getServiceNonNull(BuilderConfiguration.class);

        List<LogEntryWithContext> candidates = Lists.newArrayList();

        while (true)
        {
            candidates.clear();

            int queued = 0;

            synchronized (m_logEntries)
            {
                int threshold = 500;

                for (LogEntryWithContext entry : m_logEntries)
                {
                    candidates.add(entry);
                    queued++;

                    if (--threshold == 0)
                    {
                        break;
                    }
                }
            }

            if (queued == 0)
            {
                break;
            }

            Multimap<RecordLocator<CustomerServiceRecord>, LogEntry> mapToService = ArrayListMultimap.create();
            Multimap<RecordLocator<DeploymentHostRecord>, LogEntry>  mapToHost    = ArrayListMultimap.create();

            try (SessionHolder sessionHolder = SessionHolder.createWithNewSessionWithTransaction(this, null, Optio3DbRateLimiter.HighPriority))
            {
                RecordLocked<HostRecord> lock_host = sessionHolder.getEntityWithLock(HostRecord.class, cfg.host.sysId, 30, TimeUnit.SECONDS);

                try (var logHandler = HostRecord.allocateLogHandler(lock_host))
                {
                    try (LogHolder log = logHandler.newLogHolder())
                    {
                        for (LogEntryWithContext entryWithContext : candidates)
                        {
                            LogEntry entry = entryWithContext.entry;

                            var host_ctx = LoggerFactory.getService(entryWithContext.context, DeploymentHostRecord.LoggerContext.class);
                            if (host_ctx != null)
                            {
                                var loc_host = host_ctx.getHostLocator();
                                if (loc_host != null)
                                {
                                    mapToHost.put(loc_host, entry);

                                    // Send errors to the main log as well.
                                    if (entry.level != Severity.Error)
                                    {
                                        continue;
                                    }
                                }
                            }

                            var svc_ctx = LoggerFactory.getService(entryWithContext.context, CustomerServiceRecord.LoggerContext.class);
                            if (svc_ctx != null)
                            {
                                var loc_svc = svc_ctx.getServiceLocator();
                                if (loc_svc != null)
                                {
                                    mapToService.put(loc_svc, entry);

                                    // Send errors to the main log as well.
                                    if (entry.level != Severity.Error)
                                    {
                                        continue;
                                    }
                                }
                            }

                            log.addLineSync(1, entry.timestamp, null, entry.thread, entry.selector, entry.level, entry.line);
                        }
                    }

                    logHandler.trim(100_000, 20_000, Duration.ofDays(28), Duration.ofDays(2));
                }

                sessionHolder.commit();
            }

            for (RecordLocator<CustomerServiceRecord> loc_svc : mapToService.keySet())
            {
                try (SessionHolder sessionHolder = SessionHolder.createWithNewSessionWithTransaction(this, null, Optio3DbRateLimiter.HighPriority))
                {
                    RecordLocked<CustomerServiceRecord> lock_svc = sessionHolder.fromLocatorWithLock(loc_svc, 30, TimeUnit.SECONDS);

                    try (var logHandler = CustomerServiceRecord.allocateLogHandler(lock_svc))
                    {
                        try (LogHolder log = logHandler.newLogHolder())
                        {
                            for (LogEntry entry : mapToService.get(loc_svc))
                            {
                                log.addLineSync(1, entry.timestamp, null, entry.thread, entry.selector, entry.level, entry.line);
                            }
                        }

                        logHandler.trim(20_000, 2_000, Duration.ofDays(4 * 30), Duration.ofDays(2));
                    }

                    sessionHolder.commit();
                }
            }

            for (RecordLocator<DeploymentHostRecord> loc_host : mapToHost.keySet())
            {
                try (SessionHolder sessionHolder = SessionHolder.createWithNewSessionWithTransaction(this, null, Optio3DbRateLimiter.HighPriority))
                {
                    RecordLocked<DeploymentHostRecord> lock_host = sessionHolder.fromLocatorWithLock(loc_host, 30, TimeUnit.SECONDS);

                    try (var logHandler = DeploymentHostRecord.allocateLogHandler(lock_host))
                    {
                        try (LogHolder log = logHandler.newLogHolder())
                        {
                            for (LogEntry entry : mapToHost.get(loc_host))
                            {
                                log.addLineSync(1, entry.timestamp, null, entry.thread, entry.selector, entry.level, entry.line);
                            }
                        }

                        logHandler.trim(20_000, 2_000, Duration.ofDays(4 * 30), Duration.ofDays(2));
                    }

                    sessionHolder.commit();
                }
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
