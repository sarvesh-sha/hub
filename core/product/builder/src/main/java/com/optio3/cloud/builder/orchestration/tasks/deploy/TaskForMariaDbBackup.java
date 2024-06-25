/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.deploy;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.cloud.builder.logic.deploy.DeployLogicForAgent;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.builder.persistence.customer.BackupKind;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceBackupRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.customer.DatabaseMode;
import com.optio3.cloud.builder.persistence.deployment.DeploymentAgentRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentTaskRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryImageRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryTaggedImageRecord;
import com.optio3.cloud.client.deployer.model.BatchToken;
import com.optio3.cloud.client.deployer.model.ContainerConfiguration;
import com.optio3.cloud.client.deployer.model.ContainerStatus;
import com.optio3.cloud.client.deployer.model.DeploymentAgentFeature;
import com.optio3.cloud.client.deployer.model.FileStatus;
import com.optio3.cloud.client.deployer.model.LogEntry;
import com.optio3.cloud.client.deployer.model.MountPointStatus;
import com.optio3.cloud.client.deployer.model.VolumeStatus;
import com.optio3.cloud.client.deployer.model.batch.DockerBatch;
import com.optio3.cloud.client.deployer.model.batch.DockerBatchForContainerLaunch;
import com.optio3.cloud.client.deployer.proxy.DeployerControlApi;
import com.optio3.cloud.exception.InvalidArgumentException;
import com.optio3.cloud.exception.InvalidStateException;
import com.optio3.cloud.logic.BackgroundActivityHandler;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.infra.WellKnownDockerImageLabel;
import com.optio3.serialization.Reflection;
import com.optio3.util.CollectionUtils;
import com.optio3.util.Exceptions;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

public class TaskForMariaDbBackup extends BaseTaskForMariaDb implements BackgroundActivityHandler.ICleanupOnCompleteWithSession,
                                                                        BackgroundActivityHandler.ICleanupOnFailureWithSession
{
    public static boolean ENABLE_SNAPSHOT_BACKUP = false;

    public enum State
    {
        Initialize,
        //--//
        CreateBatch,
        StartBatch,
        WaitForBatch,
        //--//
        CreateContainer,
        StartContainer,
        //--//
        WaitForBackup,
        TransferBackup
    }

    public BackupKind                                 trigger;
    public String                                     containerIdForDatabase;
    public RecordLocator<CustomerServiceBackupRecord> loc_backup;
    public Map<String, String>                        containerLabels;
    public String                                     containerId;
    public BatchToken                                 batchToken;
    public int                                        batchOffset;

    public List<LogEntry> remoteLog   = Lists.newArrayList();
    public ZonedDateTime  lastRemoteTimestamp;
    public int            waitForBackupDelay;
    public MonotonousTime idleTimeout;
    public int            attempts;
    public int            maxAttempts = 6;

    //--//

    public static ActivityWithTask scheduleTask(SessionHolder sessionHolder,
                                                DeploymentTaskRecord targetTask,
                                                BackupKind trigger,
                                                Duration timeout) throws
                                                                  Exception
    {
        Exceptions.requireNotNull(targetTask, InvalidArgumentException.class, "No task provided");

        RegistryTaggedImageRecord image = targetTask.findTaggedImage(sessionHolder.createHelper(RegistryImageRecord.class), null);
        Exceptions.requireNotNull(image, InvalidArgumentException.class, "No image associated with task");

        Exceptions.requireNotNull(image.findLabelOrDefault(WellKnownDockerImageLabel.ConfigTemplate, null), InvalidArgumentException.class, "No config template in image %s", image.getTag());

        DeploymentHostRecord  rec_targetHost  = targetTask.getDeployment();
        String                hostId          = rec_targetHost.getHostId();
        DeploymentAgentRecord rec_agentActive = rec_targetHost.findActiveAgent();
        Exceptions.requireNotNull(rec_agentActive, InvalidStateException.class, "No active agent on host '%s'", hostId);

        Exceptions.requireTrue(rec_targetHost.hasRole(DeploymentRole.database), InvalidStateException.class, "No database role associated with host '%s'", hostId);

        CustomerServiceRecord rec_svc = rec_targetHost.getCustomerService();
        DatabaseMode          dbMode  = rec_svc.getDbMode();
        switch (dbMode)
        {
            case MariaDB:
                break;

            default:
                throw Exceptions.newIllegalArgumentException("Can't deploy to host '%s', service is not associated with MariaDB", hostId);
        }

        RecordLocked<DeploymentHostRecord> lock_targetHost = sessionHolder.optimisticallyUpgradeToLocked(rec_targetHost, 2, TimeUnit.MINUTES);
        Map<String, String>                containerLabels = Maps.newHashMap();

        BackgroundActivityRecord rec_activity = BaseDeployTask.scheduleActivity(lock_targetHost, rec_svc, DeploymentRole.database, null, TaskForMariaDbBackup.class, (t) ->
        {
            t.loggerInstance.info("MariaDb Backup on host '%s' with image '%s'", hostId, image.getTag());

            t.initializeTimeout(timeout);

            t.loc_image = sessionHolder.createLocator(image);
            t.tag_image = image.getTag();

            t.containerIdForDatabase = targetTask.getDockerId();

            t.containerLabels = containerLabels;
            WellKnownDockerImageLabel.DeploymentPurpose.setValue(t.containerLabels, DeploymentRole.database_backup.name());
            WellKnownDockerImageLabel.DeploymentContextId.setValue(t.containerLabels, hostId);
            WellKnownDockerImageLabel.DeploymentInstanceId.setValue(t.containerLabels, t.uniqueId);

            t.trigger = trigger;
        });

        return trackActivityWithTask(lock_targetHost, rec_activity, image, containerLabels);
    }

    //--//

    @Override
    public void configureContext()
    {
        loggerInstance = CustomerServiceRecord.buildContextualLogger(loggerInstance, getTargetServiceLocator());
    }

    @Override
    public String getTitle()
    {
        return String.format("Create MariaDB backup for host '%s'", getHostDisplayName());
    }

    @BackgroundActivityMethod(stateClass = State.class, initial = true, autoRetry = true)
    public CompletableFuture<Void> state_Initialize() throws
                                                      Exception
    {
        DeployLogicForAgent agentLogic = getLogicForAgent();

        try
        {
            List<VolumeStatus> volumes = await(agentLogic.listVolumes());
            for (VolumeStatus volume : CollectionUtils.asEmptyCollectionIfNull(volumes))
            {
                await(agentLogic.deleteVolume(volume.name, false));
            }
        }
        catch (Throwable t)
        {
            loggerInstance.debug("Failed to prune volumes, due to %s", t);
        }

        try
        {
            long neededFreeSpace = estimateRequiredDiskSpace();
            if (neededFreeSpace > 0)
            {
                DeployerControlApi proxy = await(agentLogic.getProxy(DeployerControlApi.class, 30));

                while (!agentLogic.hasEnoughFreeDisk(neededFreeSpace))
                {
                    List<FileStatus> files = await(proxy.listFiles("."));
                    files.sort(Comparator.comparing(f -> f.creationTime));

                    FileStatus oldestFile = CollectionUtils.firstElement(files);
                    if (oldestFile == null)
                    {
                        return markAsFailed("Not enough space on disk, needing at least %,d bytes", neededFreeSpace);
                    }

                    loggerInstance.warn("Needing %,d bytes of disk, only %,d available, deleting '%s'!", neededFreeSpace, agentLogic.agent_details.diskFree, oldestFile.name);

                    await(proxy.deleteFile(oldestFile.name));
                    await(proxy.flushHeartbeat());

                    agentLogic = getLogicForAgent();
                }
            }
        }
        catch (Throwable t)
        {
            loggerInstance.debug("Failed to ensure enough disk space, due to %s", t);
            return rescheduleSleeping(10, TimeUnit.SECONDS);
        }

        if (agentLogic.canSupport(DeploymentAgentFeature.DockerBatch, DeploymentAgentFeature.DockerBatchForContainerLaunch))
        {
            return continueAtState(State.CreateBatch);
        }
        else
        {
            return continueAtState(State.CreateContainer);
        }
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_CreateContainer() throws
                                                           Exception
    {
        DeployLogicForAgent    agentLogic = getLogicForAgent();
        ContainerConfiguration config     = await(prepareContainerConfig(agentLogic));

        containerId = await(agentLogic.createContainer("MariaDB-Backup-" + uniqueId, config));

        return continueAtState(State.StartContainer);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_StartContainer() throws
                                                          Exception
    {
        DeployLogicForAgent agentLogic = getLogicForAgent();
        await(agentLogic.startContainer(containerId));

        // Wait for the typical boot time before ping it.
        return continueAtState(State.WaitForBackup, 15, TimeUnit.SECONDS);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_CreateBatch() throws
                                                       Exception
    {
        DeployLogicForAgent agentLogic = getLogicForAgent();

        List<DockerBatch> list = Lists.newArrayList();

        {
            DockerBatchForContainerLaunch item = new DockerBatchForContainerLaunch();
            item.name   = "MariaDBBackup-" + uniqueId;
            item.config = await(prepareContainerConfig(agentLogic));

            list.add(item);
        }

        batchToken  = await(agentLogic.prepareBatch(list));
        batchOffset = 0;

        reportAgentReachable("Finally able to contact agent");

        return continueAtState(State.StartBatch);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_StartBatch() throws
                                                      Exception
    {
        DeployLogicForAgent agentLogic = getLogicForAgent();
        DockerBatch.Report  res        = await(agentLogic.startBatch(batchToken));

        reportAgentReachable("Finally able to contact agent");

        if (res == null)
        {
            batchToken = null;
            return continueAtState(State.CreateBatch, 10, TimeUnit.SECONDS);
        }
        else
        {
            return continueAtState(State.WaitForBatch);
        }
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_WaitForBatch() throws
                                                        Exception
    {
        DeployLogicForAgent agentLogic = getLogicForAgent();
        DockerBatch.Report res = await(agentLogic.checkBatch(batchToken, batchOffset, (line) ->
        {
            loggerInstance.info("%s: %s", getHostDisplayName(), line.payload);
            batchOffset++;
        }));

        reportAgentReachable("Finally able to contact agent");

        if (res == null)
        {
            batchToken = null;
            return continueAtState(State.CreateBatch);
        }

        if (res.results == null)
        {
            return rescheduleDelayed(100, TimeUnit.MILLISECONDS);
        }

        for (DockerBatch.BaseResult result : res.results)
        {
            if (result.failure != null)
            {
                loggerInstance.error("Batch for agent failed on host '%s': %s", getHostDisplayName(), result.failure);
                return markAsFailed(result.failure);
            }

            DockerBatchForContainerLaunch.Result launchRes = Reflection.as(result, DockerBatchForContainerLaunch.Result.class);
            if (launchRes != null)
            {
                containerId = launchRes.dockerId;
            }
        }

        await(agentLogic.closeBatch(batchToken));

        if (containerId == null)
        {
            return continueAtState(State.Initialize, 30, TimeUnit.SECONDS);
        }
        else
        {
            return continueAtState(State.WaitForBackup);
        }
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_WaitForBackup() throws
                                                         Exception
    {
        DeployLogicForAgent agentLogic = getLogicForAgent();

        class MonitorExecutionImpl extends DeployLogicForAgent.MonitorExecution
        {
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
                resetTimeout();
            }

            @Override
            public boolean processLine(LogEntry en)
            {
                remoteLog.add(en);
                return false;
            }

            @Override
            public void processExitCode(int exitCode)
            {
                this.exitCode = exitCode;
            }

            private void resetTimeout()
            {
                idleTimeout = TimeUtils.computeTimeoutExpiration(2, TimeUnit.MINUTES);
            }
        }

        MonitorExecutionImpl state = new MonitorExecutionImpl();
        state.resetTimeout();

        try
        {
            await(agentLogic.monitorExecution(containerId, 100, state));
        }
        catch (Throwable t)
        {
            // Ignore failures.
        }

        if (state.exitCode != null)
        {
            if (state.exitCode == 0)
            {
                remoteLog.clear();

                return continueAtState(State.TransferBackup);
            }

            return handleRetries("Backup '%s' on host '%s' unexpectedly exited with code %s", containerIdForDatabase, getHostDisplayName(), state.exitCode);
        }

        if (TimeUtils.isTimeoutExpired(idleTimeout))
        {
            return handleRetries("Backup '%s' on host '%s' failed to check-in", containerIdForDatabase, getHostDisplayName());
        }

        waitForBackupDelay = Math.min(60, waitForBackupDelay * 2);
        waitForBackupDelay = Math.max(5, waitForBackupDelay);

        // Wait a bit longer.
        return rescheduleDelayed(waitForBackupDelay, TimeUnit.SECONDS);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_TransferBackup() throws
                                                          Exception
    {
        loc_backup = await(beginTransferBackupFromMariaDb(containerId, VOLUME_SCRATCH.resolve(MARIADB_DATABASE_LOCATION), trigger));
        if (loc_backup == null)
        {
            return markAsFailed("Backup failed, target file was not present on MariaDB");
        }

        return markAsCompleted();
    }

    @Override
    public void cleanupOnFailure(SessionHolder sessionHolder,
                                 Throwable t) throws
                                              Exception
    {
        cleanupContainer(sessionHolder);
    }

    @Override
    public void cleanupOnComplete(SessionHolder sessionHolder) throws
                                                               Exception
    {
        cleanupContainer(sessionHolder);
    }

    private CompletableFuture<Void> handleRetries(String fmt,
                                                  Object... args) throws
                                                                  Exception
    {
        loggerInstance.error(fmt, args);

        for (LogEntry en : remoteLog)
        {
            loggerInstance.error("MariaDB Backup: %s - %s", getHostDisplayName(), en.line);
        }
        remoteLog.clear();

        if (++attempts > maxAttempts)
        {
            return markAsFailed(fmt, args);
        }

        callUnderTransaction(this::cleanupContainer);

        idleTimeout        = TimeUtils.computeTimeoutExpiration(2, TimeUnit.MINUTES);
        waitForBackupDelay = 0;

        // Retry in a few minutes.
        return continueAtState(State.Initialize, 5, TimeUnit.MINUTES);
    }

    private void cleanupContainer(SessionHolder sessionHolder)
    {
        // Do *not* delete volumes, we borrow them from the actual DB container
        tryToTerminate(null, containerId, false);
        containerId = null;
        batchToken  = null;
    }

    private CompletableFuture<ContainerConfiguration> prepareContainerConfig(DeployLogicForAgent agentLogic) throws
                                                                                                             Exception
    {
        ContainerStatus databaseState = await(agentLogic.inspectContainer(containerIdForDatabase));

        ContainerConfiguration config = new ContainerConfiguration();
        config.image = databaseState.image;

        // Mount Config and Data volumes from the database container.
        for (MountPointStatus mountPoint : databaseState.mountPoints)
        {
            if (mountPoint.isVolume())
            {
                if (StringUtils.equals(mountPoint.destination, VOLUME_CONFIG.toString()))
                {
                    config.addBind(mountPoint.name, VOLUME_CONFIG);
                }

                if (StringUtils.equals(mountPoint.destination, VOLUME_DATA.toString()))
                {
                    config.addBind(mountPoint.name, VOLUME_DATA);
                }
            }
        }

        joinNetworkIfNeeded(config, false);

        config.overrideEntrypoint("optio3-entrypoint.sh");
        config.overrideCommandLine("backup " + VOLUME_SCRATCH.toString());

        if (ENABLE_SNAPSHOT_BACKUP)
        {
            callInReadOnlySession(sessionHolder ->
                                  {
                                      RecordHelper<RegistryImageRecord> helper = sessionHolder.createHelper(RegistryImageRecord.class);
                                      RegistryImageRecord               rec    = RegistryImageRecord.findBySha(helper, config.image);
                                      if (rec != null)
                                      {
                                          Set<String> features = rec.getLabel(WellKnownDockerImageLabel.ServiceFeatures);
                                          if (features != null && features.contains("snapshot"))
                                          {
                                              config.overrideCommandLine("snapshot " + VOLUME_SCRATCH.toString());
                                          }
                                      }
                                  });
        }

        return wrapAsync(config);
    }

    //--//

    private long estimateRequiredDiskSpace() throws
                                             Exception
    {
        return withLocatorReadonly(getTargetServiceLocator(), (sessionHolder, rec_svc) ->
        {
            CustomerServiceBackupRecord rec_backup = rec_svc.findLatestBackup();

            if (rec_backup == null)
            {
                return 0L;
            }

            // Make sure we have three times the space of the last backup.
            return rec_backup.getFileSize() * 3;
        });
    }
}
