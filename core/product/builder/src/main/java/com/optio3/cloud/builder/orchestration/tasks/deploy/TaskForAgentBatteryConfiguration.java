/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.deploy;

import static com.optio3.asyncawait.CompileTime.await;

import java.util.concurrent.CompletableFuture;

import com.optio3.cloud.builder.logic.deploy.DeployLogicForAgent;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.client.deployer.model.DeployerShutdownConfiguration;
import com.optio3.cloud.client.deployer.model.DeploymentAgentFeature;
import com.optio3.cloud.client.deployer.proxy.DeployerControlApi;
import com.optio3.cloud.exception.InvalidArgumentException;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.util.Exceptions;

public class TaskForAgentBatteryConfiguration extends BaseTaskForAgent
{
    public static BackgroundActivityRecord scheduleTask(RecordLocked<DeploymentHostRecord> lock_targetHost) throws
                                                                                                            Exception
    {
        Exceptions.requireNotNull(lock_targetHost, InvalidArgumentException.class, "No host provided");

        return BaseDeployTask.scheduleActivity(lock_targetHost, null, TaskForAgentBatteryConfiguration.class, (t) ->
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
        return String.format("Change Battery Configuration on host '%s'", getHostDisplayName());
    }

    @BackgroundActivityMethod(autoRetry = true)
    public CompletableFuture<Void> process() throws
                                             Exception
    {
        DeployLogicForAgent agentLogic = getLogicForAgent();
        if (agentLogic.canSupport(DeploymentAgentFeature.ShutdownOnLowVoltage))
        {
            DeployerControlApi proxy = await(agentLogic.getProxy(DeployerControlApi.class, 100));

            String failure = await(proxy.setShutdownConfiguration(getShutdownConfiguration()));
            if (failure != null)
            {
                loggerInstance.error("Failed to change battery configuration on %s: %s", getHostDisplayName(), failure);
            }
        }

        return markAsCompleted();
    }

    private DeployerShutdownConfiguration getShutdownConfiguration() throws
                                                                     Exception
    {
        return withLocatorReadonly(getTargetHostLocator(), (sessionHolder, rec_host) ->
        {
            DeployerShutdownConfiguration cfg = rec_host.getBatteryThresholds();
            if (cfg == null)
            {
                cfg = new DeployerShutdownConfiguration();
            }

            return cfg;
        });
    }
}
