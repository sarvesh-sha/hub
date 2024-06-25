/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.deploy;

import static com.optio3.asyncawait.CompileTime.await;

import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.Maps;
import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.builder.logic.deploy.DeployLogicForAgent;
import com.optio3.cloud.builder.model.deployment.DeploymentTaskConfiguration;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentAgentRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryTaggedImageRecord;
import com.optio3.cloud.client.deployer.model.ContainerConfiguration;
import com.optio3.cloud.exception.InvalidArgumentException;
import com.optio3.cloud.exception.InvalidStateException;
import com.optio3.cloud.logic.BackgroundActivityHandler;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.infra.WellKnownDockerImageLabel;
import com.optio3.util.Exceptions;

public class TaskForTestCreation extends BaseDeployTask implements BackgroundActivityHandler.ICleanupOnFailure
{
    public enum State
    {
        PullImage,
        CreateScratchVolume,
        CreateConfigVolume,
        CreateContainer,
        StartContainer
    }

    public DeploymentTaskConfiguration              cfg;
    public RecordLocator<RegistryTaggedImageRecord> loc_image;
    public Map<String, String>                      containerLabels;
    public String                                   containerId;

    //--//

    public static ActivityWithTask scheduleTask(RecordLocked<DeploymentHostRecord> lock_targetHost,
                                                RegistryTaggedImageRecord image,
                                                DeploymentTaskConfiguration cfg) throws
                                                                                 Exception
    {
        Exceptions.requireNotNull(lock_targetHost, InvalidArgumentException.class, "No host provided");

        SessionHolder        sessionHolder  = lock_targetHost.getSessionHolder();
        DeploymentHostRecord rec_targetHost = lock_targetHost.get();

        String                hostId          = rec_targetHost.getHostId();
        DeploymentAgentRecord rec_agentActive = rec_targetHost.findActiveAgent();
        Exceptions.requireNotNull(rec_agentActive, InvalidStateException.class, "No active agent on host '%s'", hostId);

        Map<String, String> containerLabels = Maps.newHashMap();

        BackgroundActivityRecord rec_activity = BaseDeployTask.scheduleActivity(lock_targetHost, null, TaskForTestCreation.class, (t) ->
        {
            t.loggerInstance.info("Provisioning Test on host '%s' with image '%s'", hostId, image.getTag());

            t.loc_image = sessionHolder.createLocator(image);

            t.cfg = cfg;

            t.imageTag   = image.getTag();
            t.instanceId = t.uniqueId;

            t.containerLabels = containerLabels;
            WellKnownDockerImageLabel.DeploymentPurpose.setValue(t.containerLabels, "test");
            WellKnownDockerImageLabel.DeploymentContextId.setValue(t.containerLabels, hostId);
            WellKnownDockerImageLabel.DeploymentInstanceId.setValue(t.containerLabels, t.uniqueId);
        });

        return trackActivityWithTask(lock_targetHost, rec_activity, image, containerLabels);
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
        return String.format("Run Test on host '%s' using image '%s': %s", getHostDisplayName(), imageTag, cfg.commandLine);
    }

    @BackgroundActivityMethod(stateClass = State.class, initial = true, autoRetry = true)
    public CompletableFuture<Void> state_PullImage() throws
                                                     Exception
    {
        if (await(handlePullImage(loc_image)))
        {
            return AsyncRuntime.NullResult;
        }

        // Reschedule to persist state.
        return continueAtState(State.CreateScratchVolume);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_CreateScratchVolume() throws
                                                               Exception
    {
        DeployLogicForAgent agentLogic = getLogicForAgent();
        await(agentLogic.createVolume(scratchVolumeName, containerLabels));

        return continueAtState(State.CreateConfigVolume);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_CreateConfigVolume() throws
                                                              Exception
    {
        DeployLogicForAgent agentLogic = getLogicForAgent();
        await(agentLogic.createVolume(configVolumeName, containerLabels));

        return continueAtState(State.CreateContainer);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_CreateContainer() throws
                                                           Exception
    {
        ContainerConfiguration config = new ContainerConfiguration();
        config.labels = containerLabels;

        config.image = imageTag;
        config.overrideEntrypoint(cfg.entrypoint);
        config.overrideCommandLine(cfg.commandLine);

        config.privileged  = cfg.privileged;
        config.networkMode = cfg.useHostNetwork ? "host" : null;

        config.addBind(configVolumeName, Paths.get("/optio3-config"));
        config.addBind(scratchVolumeName, Paths.get("/optio3-scratch"));
//                config.addPort(8080, 8080, false);

        DeployLogicForAgent agentLogic = getLogicForAgent();
        containerId = await(agentLogic.createContainer("Test-" + uniqueId, config));

        return continueAtState(State.StartContainer);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_StartContainer() throws
                                                          Exception
    {
        DeployLogicForAgent agentLogic = getLogicForAgent();
        await(agentLogic.startContainer(containerId));

        return markAsCompleted();
    }

    @Override
    public void cleanupOnFailure(Throwable t)
    {
        // Don't wait.
        tryToTerminate();
    }

    //--//

    private CompletableFuture<Void> tryToTerminate()
    {
        try
        {
            DeployLogicForAgent agentLogic = getLogicForAgent();

            await(agentLogic.removeContainer(containerId));

            await(agentLogic.deleteVolume(configVolumeName, true));
            await(agentLogic.deleteVolume(scratchVolumeName, true));
        }
        catch (Throwable t)
        {
            loggerInstance.error("Caught exception on cleanup: %s", t);
        }

        return AsyncRuntime.NullResult;
    }
}
