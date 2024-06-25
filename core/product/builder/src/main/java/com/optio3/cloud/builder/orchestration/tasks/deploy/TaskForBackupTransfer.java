/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.deploy;

import static com.optio3.asyncawait.CompileTime.await;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.optio3.cloud.builder.logic.deploy.DeployLogicForAgent;
import com.optio3.cloud.builder.persistence.customer.BackupKind;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceBackupRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentAgentRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.exception.InvalidArgumentException;
import com.optio3.cloud.exception.InvalidStateException;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.util.Exceptions;

public class TaskForBackupTransfer extends BaseTaskForMariaDb
{
    public RecordLocator<CustomerServiceBackupRecord> loc_backup;

    //--//

    public static void scheduleTask(SessionHolder sessionHolder,
                                    DeploymentHostRecord targetHost,
                                    CustomerServiceBackupRecord backup,
                                    Duration timeout) throws
                                                      Exception
    {
        Exceptions.requireNotNull(targetHost, InvalidArgumentException.class, "No host provided");
        Exceptions.requireNotNull(backup, InvalidArgumentException.class, "No backup provided");

        String                hostId          = targetHost.getHostId();
        DeploymentAgentRecord rec_agentActive = targetHost.findActiveAgent();
        Exceptions.requireNotNull(rec_agentActive, InvalidStateException.class, "No active agent on host '%s'", hostId);

        RecordLocked<DeploymentHostRecord> lock_targetHost = sessionHolder.optimisticallyUpgradeToLocked(targetHost, 2, TimeUnit.MINUTES);

        // No delay for host migration.
        Duration delay = backup.getTrigger() == BackupKind.HostMigration ? null : Duration.of(30, ChronoUnit.SECONDS);
        BaseDeployTask.scheduleActivity(lock_targetHost, backup.getCustomerService(), (DeploymentRole[]) null, delay, TaskForBackupTransfer.class, (t) ->
        {
            t.initializeTimeout(timeout);

            t.loc_backup = sessionHolder.createLocator(backup);
        });
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
        return String.format("Transfer backup from host '%s'", getHostDisplayName());
    }

    @BackgroundActivityMethod(autoRetry = true)
    public CompletableFuture<Void> process() throws
                                             Exception
    {
        String fileToTransfer = withLocatorReadonlyOrNull(loc_backup, (sessionHolder, rec_backup) ->
        {
            if (rec_backup != null && rec_backup.isPendingTransfer())
            {
                return rec_backup.getFileIdOnAgent();
            }

            return null;
        });

        if (fileToTransfer != null)
        {
            DeployLogicForAgent.TransferProgress<?> transferProgress = withLocatorReadonlyOrNull(loc_backup, (sessionHolder, rec_backup) ->
            {
                return createTransferTracker(loggerInstance, getHostId(), rec_backup.getFileId());
            });

            DeployLogicForAgent agentLogic = getLogicForAgent();
            boolean success = await(agentLogic.copyFileFromAgent(fileToTransfer, 60, transferProgress, (tmpFile) ->
            {
                withLocator(loc_backup, (sessionHolder, rec_backup) ->
                {
                    rec_backup.saveFileToCloud(appConfig.credentials, tmpFile);

                    rec_backup.setPendingTransfer(false);
                });

                return true;
            }));

            if (!success)
            {
                withLocator(loc_backup, (sessionHolder, rec_backup) ->
                {
                    // Since there's no file, delete the record.
                    sessionHolder.deleteEntity(rec_backup);
                });

                return markAsFailed("Backup not found");
            }
        }

        return markAsCompleted();
    }
}
