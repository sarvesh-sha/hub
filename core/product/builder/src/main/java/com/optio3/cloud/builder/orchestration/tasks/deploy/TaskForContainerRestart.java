/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.deploy;

import static com.optio3.asyncawait.CompileTime.await;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.optio3.cloud.builder.logic.deploy.DeployLogicForAgent;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentAgentRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentTaskRecord;
import com.optio3.cloud.exception.InvalidArgumentException;
import com.optio3.cloud.exception.InvalidStateException;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.util.Exceptions;

public class TaskForContainerRestart extends BaseDeployTask
{
    public boolean stopFirst;

    public static BackgroundActivityRecord scheduleTask(RecordLocked<DeploymentTaskRecord> lock_targetTask,
                                                        boolean stopFirst) throws
                                                                           Exception
    {
        Exceptions.requireNotNull(lock_targetTask, InvalidArgumentException.class, "No task provided");

        DeploymentTaskRecord targetTask = lock_targetTask.get();

        DeploymentHostRecord rec_targetHost = targetTask.getDeployment();
        Exceptions.requireNotNull(rec_targetHost, InvalidStateException.class, "No host for task");

        SessionHolder                      sessionHolder   = lock_targetTask.getSessionHolder();
        RecordLocked<DeploymentHostRecord> lock_targetHost = sessionHolder.optimisticallyUpgradeToLocked(rec_targetHost, 2, TimeUnit.MINUTES);

        String                hostId          = rec_targetHost.getHostId();
        DeploymentAgentRecord rec_agentActive = rec_targetHost.findActiveAgent();
        Exceptions.requireNotNull(rec_agentActive, InvalidStateException.class, "No active agent on host '%s'", hostId);

        String                dockerId  = targetTask.getDockerId();
        DeploymentAgentRecord rec_agent = rec_targetHost.findAgentByDockerId(dockerId);
        if (rec_agent != null && rec_agent.isActive())
        {
            throw Exceptions.newGenericException(InvalidStateException.class, "Can't restart active agent on host '%s'", hostId);
        }

        return BaseDeployTask.scheduleActivity(lock_targetHost, null, TaskForContainerRestart.class, (t) ->
        {
            t.loggerInstance.info("Restarting Container '%s' on host '%s'", dockerId, hostId);

            t.instanceId = dockerId;
            t.stopFirst  = stopFirst;
        });
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
        return String.format("Restart Container '%s' on host '%s'", instanceId, getHostDisplayName());
    }

    @BackgroundActivityMethod(autoRetry = true)
    public CompletableFuture<Void> process() throws
                                             Exception
    {
        DeployLogicForAgent agentLogic = getLogicForAgent();

        if (stopFirst)
        {
            await(agentLogic.stopContainer(instanceId));
        }

        await(agentLogic.startContainer(instanceId));

        return markAsCompleted();
    }
}
