/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.deploy;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.optio3.cloud.builder.logic.deploy.DeployLogicForAgentOnHost;
import com.optio3.cloud.builder.model.deployment.DeploymentStatus;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentAgentRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentTaskRecord;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.infra.StatusCheckResult;

public class TaskForHostTermination extends BaseHostDeployTask
{
    public static BackgroundActivityRecord scheduleTask(RecordLocked<DeploymentHostRecord> lock_targetHost,
                                                        Duration timeout) throws
                                                                          Exception
    {
        SessionHolder        sessionHolder = lock_targetHost.getSessionHolder();
        DeploymentHostRecord targetHost    = lock_targetHost.get();

        CustomerServiceRecord rec_service = targetHost.getCustomerService();

        return BaseDeployTask.scheduleActivity(lock_targetHost, rec_service, (DeploymentRole[]) null, null, TaskForHostTermination.class, (t) ->
        {
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
        return String.format("Terminate Host '%s'", getHostDisplayName());
    }

    @BackgroundActivityMethod()
    public CompletableFuture<Void> process() throws
                                             Exception
    {
        DeployLogicForAgentOnHost agentLogic = getLogicForAgentOnHost();

        switch (agentLogic.host_status)
        {
            case Ready:
            {
                loggerInstance.info("Terminating host '%s'", agentLogic.host_displayName);

                agentLogic.terminateHost();

                setHostStatus(DeploymentStatus.Terminating);
                return rescheduleDelayed(1, TimeUnit.SECONDS);
            }

            case Terminating:
            {
                if (agentLogic.checkHostShutdown() != StatusCheckResult.Positive)
                {
                    return rescheduleDelayed(1, TimeUnit.SECONDS);
                }

                loggerInstance.info("Terminated host '%s'", agentLogic.host_displayName);

                //
                // Mark all the agents and tasks as dead.
                //
                lockedWithLocator(getTargetHostLocator(), 2, TimeUnit.MINUTES, (sessionHolder, lock_host) ->
                {
                    DeploymentHostRecord rec_host = lock_host.get();

                    for (DeploymentAgentRecord rec_agent : rec_host.getAgents())
                    {
                        rec_agent.setActive(false);
                        rec_agent.setStatus(DeploymentStatus.Terminated);
                    }

                    for (DeploymentTaskRecord rec_task : rec_host.getTasks())
                    {
                        rec_task.setDockerId(null);
                        rec_task.setStatus(DeploymentStatus.Terminated);
                    }
                });

                setHostStatus(DeploymentStatus.Terminated);
                return markAsCompleted();
            }

            default:
                return markAsCompleted();
        }
    }
}
