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
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentAgentRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentTaskRecord;
import com.optio3.cloud.client.deployer.model.BatchToken;
import com.optio3.cloud.client.deployer.model.ContainerStatus;
import com.optio3.cloud.client.deployer.model.DeploymentAgentFeature;
import com.optio3.cloud.client.deployer.model.batch.DockerBatch;
import com.optio3.cloud.client.deployer.model.batch.DockerBatchForContainerTerminate;
import com.optio3.cloud.exception.InvalidArgumentException;
import com.optio3.cloud.exception.InvalidStateException;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.util.Exceptions;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;

public class TaskForContainerTermination extends BaseDeployTask
{
    public enum State
    {
        WaitForShutdown,
        TerminateContainer,
        //--//
        CreateBatch,
        StartBatch,
        WaitForBatch
    }

    public RecordLocator<DeploymentTaskRecord> loc_task;
    public MonotonousTime                      waitForShutdown;
    public String                              containerId;
    public BatchToken                          batchToken;
    public int                                 batchOffset;
    public int                                 nextCheckin;

    //--//

    public static BackgroundActivityRecord scheduleTask(SessionHolder sessionHolder,
                                                        DeploymentTaskRecord targetTask,
                                                        int waitForShutdown) throws
                                                                             Exception
    {
        Exceptions.requireNotNull(targetTask, InvalidArgumentException.class, "No task provided");

        final String targetDockerId = targetTask.getDockerId();
        Exceptions.requireNotNull(targetDockerId, InvalidArgumentException.class, "Task has no docker id");

        DeploymentHostRecord rec_targetHost = targetTask.getDeployment();
        Exceptions.requireNotNull(rec_targetHost, InvalidStateException.class, "No host for task");

        RecordLocked<DeploymentHostRecord> lock_targetHost = sessionHolder.optimisticallyUpgradeToLocked(rec_targetHost, 2, TimeUnit.MINUTES);

        String                hostId          = rec_targetHost.getHostId();
        DeploymentAgentRecord rec_agentActive = rec_targetHost.findActiveAgent();
        Exceptions.requireNotNull(rec_agentActive, InvalidStateException.class, "No active agent on host '%s'", hostId);

        if (rec_targetHost.findAgentByDockerId(targetDockerId) != null)
        {
            throw Exceptions.newGenericException(InvalidStateException.class, "Can't terminate agent on host '%s'", hostId);
        }

        return BaseDeployTask.scheduleActivity(lock_targetHost, null, TaskForContainerTermination.class, (t) ->
        {
            t.loggerInstance.info("Terminating Container '%s' on host '%s'", targetDockerId, hostId);

            t.instanceId  = targetDockerId;
            t.containerId = targetDockerId;
            t.loc_task    = sessionHolder.createLocator(targetTask);

            if (waitForShutdown > 0)
            {
                t.waitForShutdown = TimeUtils.computeTimeoutExpiration(waitForShutdown, TimeUnit.SECONDS);
            }
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
        return String.format("Terminate Container '%s' on host '%s'", instanceId, getHostDisplayName());
    }

    @BackgroundActivityMethod(stateClass = State.class, initial = true, autoRetry = true)
    public CompletableFuture<Void> state_WaitForShutdown() throws
                                                           Exception
    {
        DeployLogicForAgent agentLogic = getLogicForAgent();

        if (!TimeUtils.isTimeoutExpired(waitForShutdown))
        {
            boolean readyForCleanup;

            try
            {
                ContainerStatus state = await(agentLogic.inspectContainer(containerId));

                readyForCleanup = !state.running;
            }
            catch (Throwable t)
            {
                readyForCleanup = false;
            }

            if (!readyForCleanup)
            {
                loggerInstance.info("Waiting to remove Container '%s' on host '%s', still running...", containerId, getHostDisplayName());
                return rescheduleDelayed(5, TimeUnit.SECONDS);
            }
        }

        if (agentLogic.canSupport(DeploymentAgentFeature.DockerBatch, DeploymentAgentFeature.DockerBatchForContainerTerminate))
        {
            return continueAtState(State.CreateBatch);
        }
        else
        {
            return continueAtState(State.TerminateContainer);
        }
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_TerminateContainer() throws
                                                              Exception
    {
        DeployLogicForAgent agentLogic = getLogicForAgent();

        loggerInstance.info("Removing Container '%s' on host '%s'", containerId, agentLogic.host_displayName);

        await(removeContainerAndVolumes(agentLogic, containerId));

        loggerInstance.info("Removed Container '%s' from host '%s'", containerId, agentLogic.host_displayName);

        return markAsCompleted();
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
            batchToken = null;
            return continueAtState(State.CreateBatch, 1, TimeUnit.SECONDS);
        }
        else
        {
            return continueAtState(State.WaitForBatch, 1, TimeUnit.SECONDS);
        }
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

        if (res == null)
        {
            batchToken = null;
            return continueAtState(State.CreateBatch, 1, TimeUnit.SECONDS);
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
                loggerInstance.error("Batch for agent failed on host '%s': %s", agentLogic.host_displayName, result.failure);
                return markAsFailed(result.failure);
            }
        }

        await(agentLogic.closeBatch(batchToken));

        return markAsCompleted();
    }
}
