/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.deploy;

import static com.optio3.asyncawait.CompileTime.await;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.optio3.cloud.builder.logic.deploy.DeployLogicForAgent;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentAgentRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.client.deployer.proxy.DeployerControlApi;
import com.optio3.cloud.exception.InvalidArgumentException;
import com.optio3.cloud.exception.InvalidStateException;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.util.Exceptions;

public class TaskForBootOptionsPull extends BaseTaskForAgent
{
    public static BackgroundActivityRecord scheduleTask(RecordLocked<DeploymentHostRecord> lock_targetHost) throws
                                                                                                            Exception
    {
        Exceptions.requireNotNull(lock_targetHost, InvalidArgumentException.class, "No host provided");

        DeploymentHostRecord  targetHost      = lock_targetHost.get();
        String                hostId          = targetHost.getHostId();
        DeploymentAgentRecord rec_agentActive = targetHost.findActiveAgent();
        Exceptions.requireNotNull(rec_agentActive, InvalidStateException.class, "No active agent on host '%s'", hostId);

        return BaseDeployTask.scheduleActivity(lock_targetHost, null, TaskForBootOptionsPull.class, (t) ->
        {
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
        return String.format("Pull boot options on host '%s'", getHostDisplayName());
    }

    @BackgroundActivityMethod(autoRetry = true)
    public CompletableFuture<Void> process() throws
                                             Exception
    {
        DeployLogicForAgent agentLogic = getLogicForAgent();
        DeployerControlApi  proxy      = await(agentLogic.getProxy(DeployerControlApi.class, 100));

        Map<String, String> map = await(proxy.getBootParameters());
        if (map == null)
        {
            loggerInstance.error("Failed to fetch Boot Options...");
        }
        else
        {
            lockedWithLocator(getTargetHostLocator(), 2, TimeUnit.MINUTES, (sessionHolder, lock_host) ->
            {
                DeploymentHostRecord rec_host = lock_host.get();
                rec_host.updateBootOptions(map);
            });
        }

        return markAsCompleted();
    }
}
