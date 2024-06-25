/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.deploy;

import static com.optio3.asyncawait.CompileTime.await;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.archive.TarBuilder;
import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.logic.deploy.DeployLogicForAgent;
import com.optio3.cloud.builder.logic.deploy.DeployLogicForHub;
import com.optio3.cloud.builder.model.customer.CustomerVertical;
import com.optio3.cloud.builder.model.deployment.DeploymentHostFlavor;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceBackupRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.customer.DatabaseMode;
import com.optio3.cloud.builder.persistence.customer.EmbeddedDatabaseConfiguration;
import com.optio3.cloud.builder.persistence.deployment.DeploymentAgentRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryTaggedImageRecord;
import com.optio3.cloud.client.deployer.model.ContainerConfiguration;
import com.optio3.cloud.exception.InvalidArgumentException;
import com.optio3.cloud.exception.InvalidStateException;
import com.optio3.cloud.logic.BackgroundActivityHandler;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.infra.WellKnownDockerImageLabel;
import com.optio3.infra.WellKnownEnvironmentVariable;
import com.optio3.infra.directory.CertificateInfo;
import com.optio3.infra.directory.CredentialDirectory;
import com.optio3.util.Base64EncodedValue;
import com.optio3.util.ConfigVariables;
import com.optio3.util.Encryption;
import com.optio3.util.Exceptions;
import com.optio3.util.IConfigVariable;
import liquibase.util.StringUtils;
import org.apache.commons.io.FileUtils;

public class TaskForHubCreation extends BaseDeployTask implements BackgroundActivityHandler.ICleanupOnFailure
{
    public enum State
    {
        PullImage,
        CreateScratchVolume,
        CreateConfigVolume,
        CreateDataVolume,
        CreateContainer,
        LoadConfiguration,
        LoadBackup,
        StartContainer,
        WaitForHub
    }

    public enum ConfigVariable implements IConfigVariable
    {
        DemoData("DEMODATA"),
        DbConfig("DB_CONFIG"),
        HostId("HOST_ID"),
        BuildId("BUILD_ID"),
        RestConnectionUrl("REST_CONNECTION_URL"),
        KeyStoreLocation("KEY_STORE_LOCATION"),
        KeyStorePassword("KEY_STORE_PASSWORD"),
        MasterKey("MASTER_KEY"),
        CommunicatorConnectionUrl("COMMUNICATOR_URL"),
        CommunicatorId("COMMUNICATOR_ID"),
        CommunicatorAccessKey("COMMUNICATOR_KEY");

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

    private static final ConfigVariables.Validator<ConfigVariable> s_configValidator        = new ConfigVariables.Validator<>(ConfigVariable.values());
    private static final ConfigVariables.Template<ConfigVariable>  s_template_YamlFromBuild = s_configValidator.newTemplate(BaseTaskForMariaDb.class, null, "${", "}");

    //--//

    public RecordLocator<RegistryTaggedImageRecord>   loc_image;
    public RecordLocator<CustomerServiceBackupRecord> loc_backup;
    public boolean                                    restoreServiceSettings;
    public boolean                                    configureThroughEnvVar;
    public Map<String, String>                        containerLabels;
    public String                                     containerId;

    //--//

    public static ActivityWithTask scheduleTask(RecordLocked<DeploymentHostRecord> lock_targetHost,
                                                RegistryTaggedImageRecord image,
                                                CustomerServiceBackupRecord backup,
                                                boolean restoreServiceSettings,
                                                Duration timeout) throws
                                                                  Exception
    {
        Exceptions.requireNotNull(lock_targetHost, InvalidArgumentException.class, "No host provided");

        Exceptions.requireNotNull(image.findLabelOrDefault(WellKnownDockerImageLabel.ConfigTemplate, null), InvalidArgumentException.class, "No config template in image %s", image.getTag());

        SessionHolder        sessionHolder  = lock_targetHost.getSessionHolder();
        DeploymentHostRecord rec_targetHost = lock_targetHost.get();

        String                hostId          = rec_targetHost.getHostId();
        DeploymentAgentRecord rec_agentActive = rec_targetHost.findActiveAgent();
        Exceptions.requireNotNull(rec_agentActive, InvalidStateException.class, "No active agent on host '%s'", hostId);

        DeploymentRole role = rec_targetHost.findRoleCompatibleWithImage(image);
        Exceptions.requireNotNull(role, InvalidStateException.class, "No role associated with host '%s'", hostId);

        CustomerServiceRecord rec_svc = rec_targetHost.getCustomerService();

        //
        // Create automation password for this Hub.
        //
        if (rec_svc.getMaintPassword() == null)
        {
            BuilderConfiguration cfg = sessionHolder.getServiceNonNull(BuilderConfiguration.class);

            rec_svc.setMaintPassword(cfg.encrypt(Encryption.generateRandomKeyAsBase64()));
        }

        Map<String, String> containerLabels = Maps.newHashMap();

        BackgroundActivityRecord rec_activity = BaseDeployTask.scheduleActivity(lock_targetHost, rec_svc, role, null, TaskForHubCreation.class, (t) ->
        {
            t.loggerInstance.info("Provisioning Hub on host '%s' with image '%s'", hostId, image.getTag());

            t.initializeTimeout(timeout);

            t.loc_image              = sessionHolder.createLocator(image);
            t.loc_backup             = sessionHolder.createLocator(backup);
            t.restoreServiceSettings = restoreServiceSettings;

            t.buildId  = image.getTag();
            t.imageTag = image.getTag();

            t.containerLabels = containerLabels;
            WellKnownDockerImageLabel.DeploymentPurpose.setValue(t.containerLabels, DeploymentRole.hub.name());
            WellKnownDockerImageLabel.DeploymentContextId.setValue(t.containerLabels, hostId);
            WellKnownDockerImageLabel.DeploymentInstanceId.setValue(t.containerLabels, t.uniqueId);
        });

        return trackActivityWithTask(lock_targetHost, rec_activity, image, containerLabels);
    }

    //--//

    @Override
    public void configureContext()
    {
        loggerInstance = DeploymentHostRecord.buildContextualLogger(loggerInstance, getTargetHostLocator());
    }

    @Override
    public String getTitle()
    {
        return String.format("Create Hub on host '%s' using image '%s'", getHostDisplayName(), imageTag);
    }

    @BackgroundActivityMethod(stateClass = State.class, initial = true, autoRetry = true)
    public CompletableFuture<Void> state_PullImage() throws
                                                     Exception
    {
        if (await(handlePullImage(loc_image)))
        {
            return AsyncRuntime.NullResult;
        }

        // Reschedule to persist state.
        return continueAtState(State.CreateScratchVolume);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_CreateScratchVolume() throws
                                                               Exception
    {
        DeployLogicForAgent agentLogic = getLogicForAgent();
        await(agentLogic.createVolume(scratchVolumeName, containerLabels));

        // Reschedule to persist state.
        return continueAtState(State.CreateConfigVolume);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_CreateConfigVolume() throws
                                                              Exception
    {
        DeployLogicForAgent agentLogic = getLogicForAgent();
        await(agentLogic.createVolume(configVolumeName, containerLabels));

        // Reschedule to persist state.
        return continueAtState(State.CreateDataVolume);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_CreateDataVolume() throws
                                                            Exception
    {
        DeployLogicForAgent agentLogic = getLogicForAgent();
        await(agentLogic.createVolume(dataVolumeName, containerLabels));

        // Reschedule to persist state.
        return continueAtState(State.CreateContainer);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_CreateContainer() throws
                                                           Exception
    {
        ContainerConfiguration config2 = withLocatorReadonly(getTargetServiceLocator(), (sessionHolder, rec_svc) ->
        {
            CustomerVertical   vertical     = rec_svc.getVertical();
            DeploymentInstance instanceType = rec_svc.getInstanceType();

            ContainerConfiguration config = new ContainerConfiguration();
            config.image = imageTag;

            config.addBind(configVolumeName, VOLUME_CONFIG);
            config.addBind(scratchVolumeName, VOLUME_SCRATCH);
            config.addBind(dataVolumeName, VOLUME_DATA);

            joinNetworkIfNeeded(sessionHolder, config, false);

            DeployLogicForHub hubLogic = DeployLogicForHub.fromRecord(sessionHolder, rec_svc);

            config.addPort(hubLogic.getServicePort(), 8443, false);
            config.addPort(20443, 20443, true);
            config.labels = containerLabels;
            config.overrideEntrypoint("sh /app/docker-launch.sh");
            config.overrideCommandLine("server /optio3-config/hub-prod.yml");

            vertical.extraTcpPortsToOpen()
                    .forEach((k, v) ->
                             {
                                 config.addPort(k, v, false);
                             });

            vertical.extraUdpPortsToOpen()
                    .forEach((k, v) ->
                             {
                                 config.addPort(k, v, true);
                             });

            DeploymentHostRecord rec_host   = getTargetHostNoLock(sessionHolder);
            DeploymentHostFlavor hostFlavor = rec_host.classifyHost(DeploymentHostFlavor.VirtualMachine);

            int maxMemory = hostFlavor.getMaxHeapMemory(rec_host, DeploymentRole.hub, instanceType.memory);
            if (maxMemory > 0)
            {
                WellKnownEnvironmentVariable.MaxMemory.setValue(config.environmentVariables, maxMemory);
            }

            if (false) // Experimental, keep disabled.
            {
                WellKnownEnvironmentVariable.SoftReferenceKeepAliveTime.setValue(config.environmentVariables, 50);
            }

            if (configureThroughEnvVar)
            {
                configureThroughEnvVariable(config, () -> generateConfiguration("/optio3-config"));
            }

            return config;
        });

        DeployLogicForAgent agentLogic = getLogicForAgent();
        containerId = await(agentLogic.createContainer("Hub-" + uniqueId, config2));

        // Reschedule to persist state.
        return continueAtState(State.LoadConfiguration);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_LoadConfiguration() throws
                                                             Exception
    {
        if (restoreServiceSettings && loc_backup != null)
        {
            withLocator(loc_backup, (sessionHolder, rec_backup) ->
            {
                CustomerServiceRecord rec_svc = getTargetService(sessionHolder);

                rec_backup.restoreSettings(rec_svc);
            });
        }

        if (!configureThroughEnvVar)
        {
            DeployLogicForAgent agentLogic = getLogicForAgent();
            await(agentLogic.restoreFileSystem(containerId, VOLUME_CONFIG, 2, (file) -> generateConfiguration(file), null));
        }

        // Reschedule to persist state.
        return continueAtState(State.LoadBackup);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_LoadBackup() throws
                                                      Exception
    {
        if (loc_backup != null)
        {
            EmbeddedDatabaseConfiguration db = withLocatorReadonly(getTargetServiceLocator(), (sessionHolder, rec_svc) ->
            {
                return rec_svc.getDbConfiguration();
            });

            DatabaseMode backupMode = withLocatorReadonly(loc_backup, (sessionHolder, rec_backup) ->
            {
                return rec_backup.getMetadata(CustomerServiceBackupRecord.WellKnownMetadata.db_mode);
            });

            if (backupMode != db.getMode())
            {
                return markAsFailed("Incompatible backup format: expecting %s, got %s", db.getMode(), backupMode);
            }

            switch (backupMode)
            {
                case H2OnDisk:
                    DeployLogicForAgent.TransferProgress<RecordLocator<CustomerServiceBackupRecord>> transferProgress = withLocatorReadonlyOrNull(loc_backup, (sessionHolder, rec_backup) ->
                    {
                        return createTransferTracker(loggerInstance, getHostId(), rec_backup.getFileId());
                    });

                    DeployLogicForHub hubLogic = getLogicForHub();
                    await(hubLogic.transferBackupToHub(loc_backup, containerId, VOLUME_DATA.resolve(DeployLogicForHub.H2_DATABASE_LOCATION), transferProgress));
                    break;

                default:
                    return markAsFailed("Restoring Hub backup for database %s not implemented yet", db.getMode());
            }
        }

        // Reschedule to persist state.
        return continueAtState(State.StartContainer);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_StartContainer() throws
                                                          Exception
    {
        DeployLogicForAgent agentLogic = getLogicForAgent();
        await(agentLogic.startContainer(containerId));

        // Wait for the typical boot time before ping it.
        return continueAtState(State.WaitForHub, 15, TimeUnit.SECONDS);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_WaitForHub() throws
                                                      Exception
    {
        DeployLogicForHub hubLogic = getLogicForHub();

        try
        {
            hubLogic.login(true); // If the Hub is not ready, the autoRetry will handle the condition.
        }
        catch (Throwable t)
        {
            return rescheduleDelayed(5, TimeUnit.SECONDS);
        }

        hubLogic.refreshAccounts();

        hubLogic.refreshSecrets();

        Boolean disable = withLocatorReadonly(getTargetServiceLocator(), (sessionHolder, rec_svc) ->
        {
            return rec_svc.getDisableServiceWorker();
        });

        hubLogic.configureServiceWorker(disable);

        hubLogic.applyVertical(loggerInstance);

        return markAsCompleted();
    }

    @Override
    public void cleanupOnFailure(Throwable t) throws
                                              Exception
    {
        // Dont' wait...
        tryToTerminate(null, containerId, true);
    }

    //--//

    private void generateConfiguration(File file) throws
                                                  Exception
    {
        FileUtils.writeByteArrayToFile(file, generateConfiguration(""));
    }

    private byte[] generateConfiguration(String root) throws
                                                      Exception
    {
        return computeInReadOnlySession(sessionHolder ->
                                        {
                                            ByteArrayOutputStream stream = new ByteArrayOutputStream();

                                            try (TarBuilder builder = new TarBuilder(stream, true))
                                            {
                                                List<String>                  dbConfig = Lists.newArrayList();
                                                CustomerServiceRecord         rec_svc  = getTargetService(sessionHolder);
                                                String                        url      = rec_svc.getUrl();
                                                EmbeddedDatabaseConfiguration db       = rec_svc.getDbConfiguration();

                                                switch (db.getMode())
                                                {
                                                    case H2InMemory:
                                                        formatDatabaseConfigurationEntry(dbConfig, "driverClass: org.h2.Driver");

                                                        formatDatabaseConfigurationEntry(dbConfig, "user: %s", db.getDatabaseUser());
                                                        formatDatabaseConfigurationEntry(dbConfig, "password: %s", appConfig.decrypt(db.getDatabasePassword()));

                                                        // Use DB2 mode for 'offset x rows' support
                                                        formatDatabaseConfigurationEntry(dbConfig, "url: jdbc:h2:mem:%s;MODE=DB2;IGNORECASE=TRUE;DB_CLOSE_ON_EXIT=FALSE", db.getDatabaseName());
                                                        break;

                                                    case H2OnDisk:
                                                        formatDatabaseConfigurationEntry(dbConfig, "driverClass: org.h2.Driver");

                                                        formatDatabaseConfigurationEntry(dbConfig, "user: %s", db.getDatabaseUser());
                                                        formatDatabaseConfigurationEntry(dbConfig, "password: %s", appConfig.decrypt(db.getDatabasePassword()));

                                                        // Use DB2 mode for 'offset x rows' support
                                                        formatDatabaseConfigurationEntry(dbConfig,
                                                                                         "url: jdbc:h2:%s/%s/%s;MODE=DB2;IGNORECASE=TRUE;DB_CLOSE_ON_EXIT=FALSE",
                                                                                         VOLUME_DATA,
                                                                                         DeployLogicForHub.H2_DATABASE_LOCATION,
                                                                                         db.getDatabaseName());
                                                        break;

                                                    case MariaDB:
                                                        formatDatabaseConfigurationEntry(dbConfig, "driverClass: org.mariadb.jdbc.Driver");

                                                        formatDatabaseConfigurationEntry(dbConfig, "user: %s", db.getDatabaseUser());
                                                        formatDatabaseConfigurationEntry(dbConfig, "password: %s", appConfig.decryptDatabasePassword(db.getDatabasePassword()));

                                                        String server = db.getServer();
                                                        if (server == null)
                                                        {
                                                            server = "localhost:3306";
                                                        }

                                                        formatDatabaseConfigurationEntry(dbConfig, "url: jdbc:mysql://%s/%s?createDatabaseIfNotExist=true", server, db.getDatabaseName());
                                                        break;
                                                }

                                                CertificateInfo certObj = appConfig.credentials.findCertificate(url);

                                                final String passPhrase = CredentialDirectory.generateRandomKey(24);
                                                byte[]       keyStore   = certObj.generateJavaKeyStore(passPhrase);

                                                ConfigVariables<ConfigVariable> parameters = s_template_YamlFromBuild.allocate();

                                                final String keyStoreDir  = "/ssl";
                                                final String keyStoreFile = "optio3.jks";
                                                final String keyStorePath = keyStoreDir + "/" + keyStoreFile;

                                                builder.addAsBytes(root + keyStoreDir, keyStoreFile, keyStore, 0440);

                                                parameters.setValue(ConfigVariable.DemoData, rec_svc.getUseDemoData());
                                                parameters.setValue(ConfigVariable.DbConfig, String.join("\n", dbConfig));
                                                parameters.setValue(ConfigVariable.HostId, getHostId());
                                                parameters.setValue(ConfigVariable.BuildId, buildId);
                                                parameters.setValue(ConfigVariable.RestConnectionUrl, url);
                                                parameters.setValue(ConfigVariable.KeyStoreLocation, keyStorePath);
                                                parameters.setValue(ConfigVariable.KeyStorePassword, passPhrase);
                                                parameters.setValue(ConfigVariable.MasterKey, appConfig.decrypt(getTargetService(sessionHolder).getMasterKey()));
                                                parameters.setValue(ConfigVariable.CommunicatorId, rec_svc.getSysId());
                                                parameters.setValue(ConfigVariable.CommunicatorAccessKey, rec_svc.getAccessKey(appConfig));

                                                RegistryTaggedImageRecord rec_taggedImage = sessionHolder.fromLocator(loc_image);
                                                Base64EncodedValue        config          = rec_taggedImage.findLabelOrDefault(WellKnownDockerImageLabel.ConfigTemplate, null);
                                                String                    input           = new String(config.getValue());

                                                String yamlFile = parameters.convert(input);

                                                String extraConfig = rec_svc.getExtraConfigLines();
                                                if (!StringUtils.isEmpty(extraConfig))
                                                {
                                                    yamlFile += "\n\n" + extraConfig;
                                                }

                                                if (rec_svc.getUseTestReporter())
                                                {
                                                    yamlFile += "\n\n" + "reporterConnectionUrl: https://reporter.dev.optio3.io\n";
                                                }

                                                builder.addAsString(root, "hub-prod.yml", yamlFile, 0440);
                                            }

                                            return stream.toByteArray();
                                        });
    }

    private static void formatDatabaseConfigurationEntry(List<String> lines,
                                                         String fmt,
                                                         Object... args)
    {
        lines.add("  " + String.format(fmt, args));
    }
}
