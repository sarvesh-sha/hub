/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.deploy;

import static com.optio3.asyncawait.CompileTime.await;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.optio3.cloud.builder.logic.deploy.DeployLogicForAgent;
import com.optio3.cloud.builder.logic.deploy.DeployLogicForHub;
import com.optio3.cloud.builder.model.deployment.DeploymentStatus;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.builder.persistence.customer.BackupKind;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceBackupRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentAgentRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentTaskRecord;
import com.optio3.cloud.exception.InvalidArgumentException;
import com.optio3.cloud.exception.InvalidStateException;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.util.Exceptions;
import org.apache.commons.lang3.StringUtils;

public class TaskForHubBackup extends BaseDeployTask
{
    public enum State
    {
        CreateBackup,
        WaitForBackup,
        TransferBackup,
        RemoveBackup
    }

    public BackupKind                                 trigger;
    public String                                     taskDockerId;
    public RecordLocator<CustomerServiceBackupRecord> loc_backup;
    public String                                     backupSysIdOnHub;
    public String                                     backupFileOnHub;

    //--//

    public static ActivityWithTask scheduleTask(SessionHolder sessionHolder,
                                                CustomerServiceRecord targetService,
                                                BackupKind trigger,
                                                Duration timeout) throws
                                                                  Exception
    {
        Exceptions.requireNotNull(targetService, InvalidArgumentException.class, "No customer service provided");

        DeploymentTaskRecord rec_task = targetService.findAnyTaskForRole(sessionHolder, DeploymentStatus.Ready, DeploymentRole.hub);
        Exceptions.requireNotNull(rec_task, InvalidStateException.class, "No running task compatible with backup requirements");

        DeploymentHostRecord               rec_targetHost  = rec_task.getDeployment();
        RecordLocked<DeploymentHostRecord> lock_targetHost = sessionHolder.optimisticallyUpgradeToLocked(rec_targetHost, 2, TimeUnit.MINUTES);

        String                hostId          = rec_targetHost.getHostId();
        DeploymentAgentRecord rec_agentActive = rec_targetHost.findActiveAgent();
        Exceptions.requireNotNull(rec_agentActive, InvalidStateException.class, "No active agent on host '%s'", hostId);

        BackgroundActivityRecord rec_activity = BaseDeployTask.scheduleActivity(lock_targetHost, targetService, DeploymentRole.hub, null, TaskForHubBackup.class, (t) ->
        {
            t.loggerInstance.info("Creating backup of Hub on host '%s'", hostId);

            t.initializeTimeout(timeout);

            t.trigger      = trigger;
            t.taskDockerId = rec_task.getDockerId();
        });

        ActivityWithTask res = new ActivityWithTask();
        res.activity = sessionHolder.createLocator(rec_activity);
        res.task     = sessionHolder.createLocator(rec_task);
        return res;
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
        return String.format("Create Hub backup for host '%s'", getHostDisplayName());
    }

    @BackgroundActivityMethod(stateClass = State.class, initial = true)
    public CompletableFuture<Void> state_CreateBackup() throws
                                                        Exception
    {
        DeployLogicForHub hubLogic = getLogicForHub();
        hubLogic.login(false);

        com.optio3.cloud.client.hub.api.AdminTasksApi adminProxy = hubLogic.createHubProxy(com.optio3.cloud.client.hub.api.AdminTasksApi.class);
        backupSysIdOnHub = adminProxy.startBackup().sysId;

        return continueAtState(State.WaitForBackup);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_WaitForBackup() throws
                                                         Exception
    {
        DeployLogicForHub hubLogic = getLogicForHub();
        hubLogic.login(false);

        com.optio3.cloud.client.hub.api.AdminTasksApi adminProxy = hubLogic.createHubProxy(com.optio3.cloud.client.hub.api.AdminTasksApi.class);
        backupFileOnHub = adminProxy.getBackup(backupSysIdOnHub);
        if (backupFileOnHub == null)
        {
            // Sleep while waiting for backup.
            return rescheduleDelayed(5, TimeUnit.SECONDS);
        }

        if (StringUtils.equals(backupFileOnHub, "<FAILED>"))
        {
            return markAsFailed("Backup failed");
        }

        return continueAtState(State.TransferBackup);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_TransferBackup() throws
                                                          Exception
    {
        DeployLogicForAgent.TransferProgress<RecordLocator<CustomerServiceBackupRecord>> transferProgress = createTransferTracker(loggerInstance, getHostId(), backupFileOnHub);

        DeployLogicForHub hubLogic = getLogicForHub();
        await(hubLogic.transferBackupFromHub(taskDockerId, Paths.get(backupFileOnHub), trigger, transferProgress));

        loc_backup = transferProgress.context;
        if (loc_backup == null)
        {
            return markAsFailed("Backup failed, target file was not present on Hub");
        }

        return continueAtState(State.RemoveBackup);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_RemoveBackup() throws
                                                        Exception
    {
        DeployLogicForHub hubLogic = getLogicForHub();
        hubLogic.login(false);

        com.optio3.cloud.client.hub.api.AdminTasksApi adminProxy = hubLogic.createHubProxy(com.optio3.cloud.client.hub.api.AdminTasksApi.class);
        adminProxy.deleteBackup(backupSysIdOnHub);

        return markAsCompleted();
    }
}
