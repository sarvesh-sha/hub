/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.deploy;

import static com.optio3.asyncawait.CompileTime.await;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Maps;
import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.builder.logic.deploy.DeployLogicForAgent;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceBackupRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.customer.DatabaseMode;
import com.optio3.cloud.builder.persistence.customer.EmbeddedDatabaseConfiguration;
import com.optio3.cloud.builder.persistence.deployment.DeploymentAgentRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryTaggedImageRecord;
import com.optio3.cloud.client.deployer.model.ContainerConfiguration;
import com.optio3.cloud.client.deployer.model.LogEntry;
import com.optio3.cloud.exception.InvalidArgumentException;
import com.optio3.cloud.exception.InvalidStateException;
import com.optio3.cloud.logic.BackgroundActivityHandler;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.infra.WellKnownDockerImageLabel;
import com.optio3.util.Exceptions;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;

public class TaskForMariaDbCreation extends BaseTaskForMariaDb implements BackgroundActivityHandler.ICleanupOnFailure
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
        WaitForStartup
    }

    public RecordLocator<CustomerServiceBackupRecord> loc_backup;
    public boolean                                    restoreServiceSettings;
    public Map<String, String>                        containerLabels;
    public String                                     containerId;

    public ZonedDateTime  lastRemoteTimestamp;
    public MonotonousTime idleTimeout;

    //--//

    public static ActivityWithTask scheduleTask(SessionHolder sessionHolder,
                                                DeploymentHostRecord targetHost,
                                                RegistryTaggedImageRecord image,
                                                CustomerServiceBackupRecord backup,
                                                boolean restoreServiceSettings,
                                                Duration timeout) throws
                                                                  Exception
    {
        Exceptions.requireNotNull(targetHost, InvalidArgumentException.class, "No host provided");

        Exceptions.requireNotNull(image.findLabelOrDefault(WellKnownDockerImageLabel.ConfigTemplate, null), InvalidArgumentException.class, "No config template in image %s", image.getTag());

        String                hostId          = targetHost.getHostId();
        DeploymentAgentRecord rec_agentActive = targetHost.findActiveAgent();
        Exceptions.requireNotNull(rec_agentActive, InvalidStateException.class, "No active agent on host '%s'", hostId);

        DeploymentRole role = targetHost.findRoleCompatibleWithImage(image);
        Exceptions.requireTrue(role == DeploymentRole.database, InvalidStateException.class, "No database role associated with host '%s'", hostId);

        CustomerServiceRecord rec_svc = targetHost.getCustomerService();
        DatabaseMode          dbMode  = rec_svc.getDbMode();
        switch (dbMode)
        {
            case MariaDB:
                break;

            default:
                throw Exceptions.newIllegalArgumentException("Can't deploy to host '%s', service is not associated with MariaDB", hostId);
        }

        if (backup != null)
        {
            DatabaseMode backupMode = backup.getMetadata(CustomerServiceBackupRecord.WellKnownMetadata.db_mode);
            if (backupMode != dbMode)
            {
                throw Exceptions.newIllegalArgumentException("Incompatible backup format: expecting %s, got %s", dbMode, backupMode);
            }
        }

        RecordLocked<DeploymentHostRecord> lock_targetHost = sessionHolder.optimisticallyUpgradeToLocked(targetHost, 2, TimeUnit.MINUTES);
        Map<String, String>                containerLabels = Maps.newHashMap();

        BackgroundActivityRecord rec_activity = BaseDeployTask.scheduleActivity(lock_targetHost, rec_svc, DeploymentRole.database, null, TaskForMariaDbCreation.class, (t) ->
        {
            t.loggerInstance.info("Provisioning MariaDb on host '%s' with image '%s'", hostId, image.getTag());

            t.initializeTimeout(timeout);

            t.loc_image              = sessionHolder.createLocator(image);
            t.loc_backup             = sessionHolder.createLocator(backup);
            t.restoreServiceSettings = restoreServiceSettings;

            t.imageTag = image.getTag();

            t.containerLabels = containerLabels;
            WellKnownDockerImageLabel.DeploymentPurpose.setValue(t.containerLabels, DeploymentRole.database.name());
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
        return String.format("Create MariaDB on host '%s' using image '%s'", getHostDisplayName(), imageTag);
    }

    @BackgroundActivityMethod(stateClass = State.class, initial = true, autoRetry = true)
    public CompletableFuture<Void> state_PullImage() throws
                                                     Exception
    {
        if (!await(handlePullImage(loc_image)))
        {
            return AsyncRuntime.NullResult;
        }

        return continueAtState(State.CreateScratchVolume);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_CreateScratchVolume() throws
                                                               Exception
    {
        DeployLogicForAgent agentLogic = getLogicForAgent();
        await(agentLogic.createVolume(scratchVolumeName, containerLabels));

        return continueAtState(State.CreateConfigVolume);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_CreateConfigVolume() throws
                                                              Exception
    {
        DeployLogicForAgent agentLogic = getLogicForAgent();
        await(agentLogic.createVolume(configVolumeName, containerLabels));

        return continueAtState(State.CreateDataVolume);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_CreateDataVolume() throws
                                                            Exception
    {
        DeployLogicForAgent agentLogic = getLogicForAgent();
        await(agentLogic.createVolume(dataVolumeName, containerLabels));

        return continueAtState(State.CreateContainer);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_CreateContainer() throws
                                                           Exception
    {
        ContainerConfiguration config2 = withLocatorReadonly(loc_image, (sessionHolder, rec_taggedImage) ->
        {
            String dbImage = rec_taggedImage.getTag();

            ContainerConfiguration config = new ContainerConfiguration();
            config.image = dbImage;

            config.addBind(configVolumeName, VOLUME_CONFIG);
            config.addBind(scratchVolumeName, VOLUME_SCRATCH);
            config.addBind(dataVolumeName, VOLUME_DATA);

            if (appConfig.developerSettings.databasePasswordOverride != null)
            {
                // Expose port 13306 on the MariaDB container, to allow external connections for testing.
                config.addPort(13306, 3306, false);
            }

            config.labels = containerLabels;
            config.overrideEntrypoint("optio3-entrypoint.sh");
            config.overrideCommandLine("run");

            joinNetworkIfNeeded(sessionHolder, config, true);
            return config;
        });

        DeployLogicForAgent agentLogic = getLogicForAgent();
        containerId = await(agentLogic.createContainer("MariaDB-" + uniqueId, config2));

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

        DeployLogicForAgent agentLogic = getLogicForAgent();
        await(agentLogic.restoreFileSystem(containerId, VOLUME_CONFIG, 2, this::generateConfiguration, null));

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

            switch (db.getMode())
            {
                case MariaDB:
                    DeployLogicForAgent.TransferProgress<RecordLocator<CustomerServiceBackupRecord>> transferProgress = withLocatorReadonlyOrNull(loc_backup, (sessionHolder, rec_backup) ->
                    {
                        return createTransferTracker(loggerInstance, getHostId(), rec_backup.getFileId());
                    });

                    await(transferBackupToMariaDb(loc_backup, containerId, VOLUME_DATA.resolve(MARIADB_DATABASE_LOCATION), transferProgress));
                    break;

                default:
                    return markAsFailed("Restoring backup for database %s not implemented yet", db.getMode());
            }
        }

        return continueAtState(State.StartContainer);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_StartContainer() throws
                                                          Exception
    {
        DeployLogicForAgent agentLogic = getLogicForAgent();
        await(agentLogic.startContainer(containerId));

        lastRemoteTimestamp = null;
        idleTimeout         = TimeUtils.computeTimeoutExpiration(30, TimeUnit.SECONDS);

        // Wait for the typical boot time before ping it.
        return continueAtState(State.WaitForStartup, 15, TimeUnit.SECONDS);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_WaitForStartup() throws
                                                          Exception
    {
        class MonitorExecutionImpl extends DeployLogicForAgent.MonitorExecution
        {
            private boolean found;

            private Integer exitCode;

            @Override
            public ZonedDateTime getLastOutput()
            {
                return lastRemoteTimestamp;
            }

            @Override
            public void setLastOutput(ZonedDateTime lastOutput)
            {
                lastRemoteTimestamp = lastOutput;
                idleTimeout         = TimeUtils.computeTimeoutExpiration(15, TimeUnit.SECONDS);
            }

            @Override
            public boolean processLine(LogEntry en)
            {
                loggerInstance.info("%s: %s", getHostDisplayName(), en.line);

                if (en.line.startsWith("STARTUP MARKER: "))
                {
                    found = true;
                    return true;
                }

                return false;
            }

            @Override
            public void processExitCode(int exitCode)
            {
                this.exitCode = exitCode;
            }
        }

        MonitorExecutionImpl state = new MonitorExecutionImpl();

        try
        {
            DeployLogicForAgent agentLogic = getLogicForAgent();
            await(agentLogic.monitorExecution(containerId, 100, state));
        }
        catch (Throwable t)
        {
            // Ignore failures.
        }

        if (state.found)
        {
            // Reschedule to persist state.
            return markAsCompleted();
        }

        if (state.exitCode != null)
        {
            throw Exceptions.newRuntimeException("Database '%s' on host '%s' unexpectedly exited with code %s", tag_image, getHostDisplayName(), state.exitCode);
        }

        if (TimeUtils.isTimeoutExpired(idleTimeout))
        {
            throw Exceptions.newRuntimeException("Database '%s' on host '%s' failed to check-in", tag_image, getHostDisplayName());
        }

        // Wait a bit longer.
        return rescheduleDelayed(2, TimeUnit.SECONDS);
    }

    @Override
    public void cleanupOnFailure(Throwable t)
    {
        // Don't wait...
        tryToTerminate(null, containerId, true);
    }
}
