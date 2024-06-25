/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.deploy;

import static com.optio3.asyncawait.CompileTime.await;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.optio3.cloud.builder.logic.deploy.DeployLogicForAgent;
import com.optio3.cloud.builder.model.deployment.DeploymentStatus;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentAgentRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.client.deployer.model.BatchToken;
import com.optio3.cloud.client.deployer.model.DeploymentAgentFeature;
import com.optio3.cloud.client.deployer.model.batch.DockerBatch;
import com.optio3.cloud.client.deployer.model.batch.DockerBatchForContainerTerminate;
import com.optio3.cloud.exception.InvalidArgumentException;
import com.optio3.cloud.exception.InvalidStateException;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.util.Exceptions;

public class TaskForAgentTermination extends BaseTaskForAgent
{
    public enum State
    {
        MarkAsTerminating,
        RemoveContainer,
        //--//
        CreateBatch,
        StartBatch,
        WaitForBatch,
        //--//
        MarkAsTerminated
    }

    public BatchToken batchToken;
    public int        batchOffset;
    public int        nextCheckin;
    public String     containerId;

    //--//

    public static BackgroundActivityRecord scheduleTask(SessionHolder sessionHolder,
                                                        DeploymentAgentRecord targetAgent) throws
                                                                                           Exception
    {
        Exceptions.requireNotNull(targetAgent, InvalidArgumentException.class, "No agent provided");

        DeploymentHostRecord rec_targetHost = targetAgent.getDeployment();
        Exceptions.requireNotNull(rec_targetHost, InvalidArgumentException.class, "No host for agent");

        String                hostId          = rec_targetHost.getHostId();
        DeploymentAgentRecord rec_agentActive = rec_targetHost.findActiveAgent();
        Exceptions.requireNotNull(rec_agentActive, InvalidStateException.class, "No active agent on host '%s'", hostId);

        RecordLocked<DeploymentHostRecord> lock_targetHost = sessionHolder.optimisticallyUpgradeToLocked(rec_targetHost, 2, TimeUnit.MINUTES);

        return BaseDeployTask.scheduleActivity(lock_targetHost, null, TaskForAgentTermination.class, (t) ->
        {
            t.loggerInstance.info("Terminating agent '%s' on host '%s'", targetAgent.getInstanceId(), hostId);

            t.loc_targetAgent = sessionHolder.createLocator(targetAgent);
            t.containerId     = targetAgent.getDockerId();

            t.instanceId = targetAgent.getInstanceId();
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
        return String.format("Terminate Agent '%s' on host '%s'", instanceId, getHostDisplayName());
    }

    @BackgroundActivityMethod(stateClass = State.class, initial = true, autoRetry = true)
    public CompletableFuture<Void> state_MarkAsTerminating() throws
                                                             Exception
    {
        if (!setAgentStatus(DeploymentStatus.Terminating))
        {
            return markAsCompleted();
        }

        if (containerId == null)
        {
            return continueAtState(State.MarkAsTerminated);
        }

        DeployLogicForAgent agentLogic = getLogicForAgent();
        if (agentLogic.canSupport(DeploymentAgentFeature.DockerBatch, DeploymentAgentFeature.DockerBatchForContainerTerminate))
        {
            return continueAtState(State.CreateBatch);
        }
        else
        {
            return continueAtState(State.RemoveContainer);
        }
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_RemoveContainer() throws
                                                           Exception
    {
        DeployLogicForAgent agentLogic = getLogicForAgent();
        await(removeContainerAndVolumes(agentLogic, containerId));

        return continueAtState(State.MarkAsTerminated);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_CreateBatch() throws
                                                       Exception
    {
        DeployLogicForAgent agentLogic = getLogicForAgent();

        List<DockerBatch> list = Lists.newArrayList();

        {
            DockerBatchForContainerTerminate item = new DockerBatchForContainerTerminate();
            item.dockerId = containerId;
            list.add(item);
        }

        batchToken  = await(agentLogic.prepareBatch(list));
        batchOffset = 0;

        reportAgentReachable("Finally able to contact agent");

        nextCheckin = 0;
        return continueAtState(State.StartBatch);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_StartBatch() throws
                                                      Exception
    {
        DeployLogicForAgent agentLogic = getLogicForAgent();
        DockerBatch.Report  res        = await(agentLogic.startBatch(batchToken));

        reportAgentReachable("Finally able to contact agent");
        nextCheckin = 1;

        if (res == null)
        {
            batchToken  = null;
            nextCheckin = 1;
            return continueAtState(State.CreateBatch);
        }

        return continueAtState(State.WaitForBatch);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_WaitForBatch() throws
                                                        Exception
    {
        DeployLogicForAgent agentLogic = getLogicForAgent();
        DockerBatch.Report res = await(agentLogic.checkBatch(batchToken, batchOffset, (line) ->
        {
            loggerInstance.info("%s: %s", agentLogic.host_displayName, line.payload);
            batchOffset++;
            nextCheckin = 1;
        }));

        reportAgentReachable("Finally able to contact agent");

        nextCheckin = 1;

        if (res == null)
        {
            batchToken = null;
            return continueAtState(State.CreateBatch);
        }

        if (res.results == null)
        {
            nextCheckin = Math.min(10, nextCheckin + 1);
            return rescheduleDelayed(nextCheckin, TimeUnit.SECONDS);
        }

        for (DockerBatch.BaseResult result : res.results)
        {
            if (result.failure != null)
            {
                loggerInstance.error("Batch for agent failed on host '%s': %s", getHostDisplayName(), result.failure);
                return markAsFailed(result.failure);
            }
        }

        await(agentLogic.closeBatch(batchToken));

        return continueAtState(State.MarkAsTerminated);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_MarkAsTerminated() throws
                                                            Exception
    {
        DeployLogicForAgent agentLogic = getLogicForAgent();
        loggerInstance.info("Removed Agent '%s' on host '%s'", agentLogic.agent_connection.instanceId, agentLogic.host_displayName);

        setAgentStatus(DeploymentStatus.Terminated);

        return markAsCompleted();
    }
}
