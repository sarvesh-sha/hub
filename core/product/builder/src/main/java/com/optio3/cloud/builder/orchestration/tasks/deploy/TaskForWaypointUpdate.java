/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.deploy;

import static com.optio3.asyncawait.CompileTime.await;

import java.util.concurrent.CompletableFuture;

import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.builder.logic.deploy.DeployLogicForAgent;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentAgentRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentTaskRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryTaggedImageRecord;
import com.optio3.cloud.exception.InvalidArgumentException;
import com.optio3.cloud.exception.InvalidStateException;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.util.Exceptions;

public class TaskForWaypointUpdate extends BaseDeployTask
{
    public enum State
    {
        PullImage,
        StopContainer
    }

    public RecordLocator<RegistryTaggedImageRecord> loc_image;
    public DeploymentRole                           targetService;

    //--//

    public static BackgroundActivityRecord scheduleTask(RecordLocked<DeploymentHostRecord> lock_targetHost,
                                                        RegistryTaggedImageRecord image) throws
                                                                                         Exception
    {
        Exceptions.requireNotNull(lock_targetHost, InvalidArgumentException.class, "No host provided");

        SessionHolder        sessionHolder  = lock_targetHost.getSessionHolder();
        DeploymentHostRecord rec_targetHost = lock_targetHost.get();

        String                hostId          = rec_targetHost.getHostId();
        DeploymentAgentRecord rec_agentActive = rec_targetHost.findActiveAgent();
        Exceptions.requireNotNull(rec_agentActive, InvalidStateException.class, "No active agent on host '%s'", hostId);

        return BaseDeployTask.scheduleActivity(lock_targetHost, null, TaskForWaypointUpdate.class, (t) ->
        {
            t.loggerInstance.info("Provisioning Waypoint on host '%s' with image '%s'", hostId, image.getTag());

            t.loc_image     = sessionHolder.createLocator(image);
            t.imageTag      = image.getTag();
            t.targetService = image.getTargetService();
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
        return String.format("Update Waypoint on host '%s' using image '%s'", getHostDisplayName(), imageTag);
    }

    @BackgroundActivityMethod(stateClass = State.class, initial = true, autoRetry = true)
    public CompletableFuture<Void> state_PullImage() throws
                                                     Exception
    {
        if (await(handlePullImage(loc_image)))
        {
            return AsyncRuntime.NullResult;
        }

        return continueAtState(State.StopContainer);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_StopContainer() throws
                                                         Exception
    {
        String containerId = withLocatorReadonly(getTargetHostLocator(), (sessionHolder, rec_host) ->
        {
            DeploymentTaskRecord rec_targetTask = rec_host.findTaskForPurpose(null, targetService, null, true);
            return rec_targetTask != null ? rec_targetTask.getDockerId() : null;
        });

        if (containerId != null)
        {
            DeployLogicForAgent agentLogic = getLogicForAgent();

            loggerInstance.info("Removing Container '%s' on host '%s'", containerId, agentLogic.host_displayName);

            await(removeContainerAndVolumes(agentLogic, containerId));

            loggerInstance.info("Removed Container '%s' from host '%s'", containerId, agentLogic.host_displayName);
        }

        return markAsCompleted();
    }
}