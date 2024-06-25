/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.deploy;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.optio3.cloud.builder.logic.deploy.DeployLogicForHub;
import com.optio3.cloud.builder.model.customer.CheckUsagesProgress;
import com.optio3.cloud.builder.model.deployment.DeploymentStatus;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentAgentRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentTaskRecord;
import com.optio3.cloud.exception.InvalidArgumentException;
import com.optio3.cloud.exception.InvalidStateException;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.logic.IBackgroundActivityProgress;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.util.Exceptions;

public class TaskForHubCheckUsages extends BaseDeployTask implements IBackgroundActivityProgress<CheckUsagesProgress>
{
    public com.optio3.cloud.client.hub.model.UsageFilterRequest  filters;
    public com.optio3.cloud.client.hub.model.UsageFilterResponse responses;

    public static BackgroundActivityRecord scheduleTask(SessionHolder sessionHolder,
                                                        CustomerServiceRecord targetService,
                                                        com.optio3.cloud.client.hub.model.UsageFilterRequest filters,
                                                        Duration timeout) throws
                                                                          Exception
    {
        Exceptions.requireNotNull(targetService, InvalidArgumentException.class, "No customer service provided");

        DeploymentTaskRecord rec_task = targetService.findAnyTaskForRole(sessionHolder, DeploymentStatus.Ready, DeploymentRole.hub);
        if (rec_task == null)
        {
            // No running task for Hub.
            return null;
        }

        DeploymentHostRecord               rec_targetHost  = rec_task.getDeployment();
        RecordLocked<DeploymentHostRecord> lock_targetHost = sessionHolder.optimisticallyUpgradeToLocked(rec_targetHost, 2, TimeUnit.MINUTES);

        String                hostId          = rec_targetHost.getHostId();
        DeploymentAgentRecord rec_agentActive = rec_targetHost.findActiveAgent();
        Exceptions.requireNotNull(rec_agentActive, InvalidStateException.class, "No active agent on host '%s'", hostId);

        return BaseDeployTask.scheduleActivity(lock_targetHost, targetService, DeploymentRole.hub, null, TaskForHubCheckUsages.class, (t) ->
        {
            t.initializeTimeout(timeout);

            t.filters = filters;
        });
    }

    //--//

    @Override
    public CheckUsagesProgress fetchProgress(SessionHolder sessionHolder,
                                             boolean detailed)
    {
        var res = new CheckUsagesProgress();
        res.results = responses;
        return res;
    }

    @Override
    public void generateStream() throws
                                 IOException
    {
        // Nothing to do.
    }

    @Override
    public InputStream streamContents() throws
                                        IOException
    {
        return null;
    }

    //--//

    @Override
    public void configureContext()
    {
        // Nothing to do.
    }

    @Override
    public String getTitle()
    {
        return "Check usages";
    }

    @BackgroundActivityMethod(autoRetry = true)
    public CompletableFuture<Void> process() throws
                                             Exception
    {
        try
        {
            DeployLogicForHub hubLogic = getLogicForHub();
            hubLogic.login(true); // If the Hub is not ready, we'll fail the activity, no problem.

            com.optio3.cloud.client.hub.api.AdminTasksApi proxy = hubLogic.createHubProxy(com.optio3.cloud.client.hub.api.AdminTasksApi.class);
            responses = proxy.checkUsages(filters);
        }
        catch (Throwable t)
        {
            loggerInstance.error("Failed to check usages, due to %s", t);
        }

        return markAsCompleted();
    }
}
