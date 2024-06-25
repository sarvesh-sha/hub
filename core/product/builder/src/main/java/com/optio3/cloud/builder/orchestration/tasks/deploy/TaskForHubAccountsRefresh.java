/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.deploy;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.optio3.cloud.builder.logic.deploy.DeployLogicForHub;
import com.optio3.cloud.builder.model.deployment.DeploymentStatus;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentAgentRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentTaskRecord;
import com.optio3.cloud.exception.InvalidArgumentException;
import com.optio3.cloud.exception.InvalidStateException;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.util.Exceptions;

public class TaskForHubAccountsRefresh extends BaseDeployTask
{
    public static BackgroundActivityRecord scheduleTask(SessionHolder sessionHolder,
                                                        CustomerServiceRecord targetService,
                                                        Duration timeout) throws
                                                                          Exception
    {
        Exceptions.requireNotNull(targetService, InvalidArgumentException.class, "No customer service provided");

        DeploymentTaskRecord rec_task = targetService.findAnyTaskForRole(sessionHolder, DeploymentStatus.Ready, DeploymentRole.hub);
        Exceptions.requireNotNull(rec_task, InvalidStateException.class, "No running task compatible with Hub requirements");

        DeploymentHostRecord               rec_targetHost  = rec_task.getDeployment();
        RecordLocked<DeploymentHostRecord> lock_targetHost = sessionHolder.optimisticallyUpgradeToLocked(rec_targetHost, 2, TimeUnit.MINUTES);

        String                hostId          = rec_targetHost.getHostId();
        DeploymentAgentRecord rec_agentActive = rec_targetHost.findActiveAgent();
        Exceptions.requireNotNull(rec_agentActive, InvalidStateException.class, "No active agent on host '%s'", hostId);

        return BaseDeployTask.scheduleActivity(lock_targetHost, targetService, DeploymentRole.hub, null, TaskForHubAccountsRefresh.class, (t) ->
        {
            t.loggerInstance.info("Refresh shared accounts on host '%s'", hostId);

            t.initializeTimeout(timeout);
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
        return String.format("Refresh shared accounts on host '%s'", getHostDisplayName());
    }

    @BackgroundActivityMethod(autoRetry = true)
    public CompletableFuture<Void> process() throws
                                             Exception
    {
        DeployLogicForHub hubLogic = getLogicForHub();
        hubLogic.login(true); // If the Hub is not ready, the autoRetry will handle the condition.

        hubLogic.refreshAccounts();

        return markAsCompleted();
    }
}