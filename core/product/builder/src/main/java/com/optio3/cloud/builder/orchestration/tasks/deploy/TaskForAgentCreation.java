/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.deploy;

import static com.optio3.asyncawait.CompileTime.await;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.JsonWebSocketDnsHints;
import com.optio3.cloud.builder.logic.deploy.DeployLogicForAgent;
import com.optio3.cloud.builder.model.deployment.DeploymentHostFlavor;
import com.optio3.cloud.builder.model.deployment.DeploymentStatus;
import com.optio3.cloud.builder.model.jobs.output.RegistryImageReleaseStatus;
import com.optio3.cloud.builder.model.jobs.output.ReleaseStatusReport;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentAgentRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryTaggedImageRecord;
import com.optio3.cloud.client.deployer.model.BatchToken;
import com.optio3.cloud.client.deployer.model.ContainerConfiguration;
import com.optio3.cloud.client.deployer.model.DeploymentAgentFeature;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.cloud.client.deployer.model.ImageStatus;
import com.optio3.cloud.client.deployer.model.batch.DockerBatch;
import com.optio3.cloud.client.deployer.model.batch.DockerBatchForContainerLaunch;
import com.optio3.cloud.client.deployer.model.batch.DockerBatchForVolumeCreate;
import com.optio3.cloud.exception.InvalidArgumentException;
import com.optio3.cloud.exception.InvalidStateException;
import com.optio3.cloud.logic.BackgroundActivityHandler;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.infra.WellKnownDockerImageLabel;
import com.optio3.infra.WellKnownEnvironmentVariable;
import com.optio3.infra.deploy.CommonDeployer;
import com.optio3.infra.deploy.StubDeployer;
import com.optio3.util.Base64EncodedValue;
import com.optio3.util.CollectionUtils;
import com.optio3.util.Exceptions;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

public class TaskForAgentCreation extends BaseTaskForAgent implements BackgroundActivityHandler.ICleanupOnFailure
{
    public enum State
    {
        PullImage,
        CreateScratchVolume,
        CreateConfigVolume,
        CreateContainer,
        LoadConfiguration,
        StartContainer,
        //--//
        CreateBatch,
        StartBatch,
        WaitForBatch,
        //--//
        WaitForAgent
    }

    public RecordLocator<RegistryTaggedImageRecord> loc_image;
    public String                                   rawTemplate;
    public boolean                                  configureThroughEnvVar;
    public Map<String, String>                      containerLabels;
    public String                                   containerId;
    public BatchToken                               batchToken;
    public int                                      batchOffset;
    public int                                      nextCheckin;
    public boolean                                  activate;

    //--//

    public static ActivityWithTask scheduleTask(RecordLocked<DeploymentHostRecord> lock_targetHost,
                                                RegistryTaggedImageRecord image,
                                                String rawImageTag,
                                                boolean activate) throws
                                                                  Exception
    {
        Exceptions.requireNotNull(lock_targetHost, InvalidArgumentException.class, "No host provided");

        SessionHolder        sessionHolder = lock_targetHost.getSessionHolder();
        DeploymentHostRecord targetHost    = lock_targetHost.get();

        String                hostId          = targetHost.getHostId();
        DeploymentAgentRecord rec_agentActive = targetHost.findActiveAgent();
        Exceptions.requireNotNull(rec_agentActive, InvalidStateException.class, "No active agent on host '%s'", hostId);

        DeploymentAgentRecord rec_agent = DeployLogicForAgent.initializeNewAgent(lock_targetHost);

        final String imageTag = image != null ? image.getTag() : rawImageTag;

        Map<String, String> containerLabels = Maps.newHashMap();

        BackgroundActivityRecord rec_activity = BaseDeployTask.scheduleActivity(lock_targetHost, null, TaskForAgentCreation.class, (t) ->
        {
            t.loggerInstance.info("Provisioning agent '%s' on host '%s' with image '%s'", rec_agent.getInstanceId(), hostId, imageTag);

            t.loc_targetAgent = sessionHolder.createLocator(rec_agent);
            t.loc_image       = sessionHolder.createLocator(image);

            t.instanceId = rec_agent.getInstanceId();
            t.imageTag   = imageTag;
            t.activate   = activate;

            t.containerLabels = containerLabels;
            WellKnownDockerImageLabel.DeploymentPurpose.setValue(t.containerLabels, DeploymentRole.deployer.name());
            WellKnownDockerImageLabel.DeploymentContextId.setValue(t.containerLabels, hostId);
            WellKnownDockerImageLabel.DeploymentInstanceId.setValue(t.containerLabels, t.instanceId);
        });

        return trackActivityWithTask(lock_targetHost, rec_activity, imageTag, containerLabels);
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
        return String.format("Create Agent '%s' on host '%s' using image '%s'", instanceId, getHostDisplayName(), imageTag);
    }

    @BackgroundActivityMethod(stateClass = State.class, initial = true, autoRetry = true)
    public CompletableFuture<Void> state_PullImage() throws
                                                     Exception
    {
        DeployLogicForAgent agentLogic = getLogicForAgent();

        if (loc_image != null)
        {
            if (await(handlePullImage(loc_image)))
            {
                return AsyncRuntime.NullResult;
            }
        }
        else
        {
            List<ImageStatus> images = await(agentLogic.listImages(false));
            for (ImageStatus image : images)
            {
                if (CollectionUtils.findFirst(image.repoTags, (tag) -> StringUtils.equals(tag, imageTag)) != null)
                {
                    if (image.labels != null)
                    {
                        Base64EncodedValue config = WellKnownDockerImageLabel.ConfigTemplate.getLabel(image.labels);
                        if (config != null)
                        {
                            rawTemplate = new String(config.getValue());
                            break;
                        }
                    }
                }
            }

            if (rawTemplate != null)
            {
                loggerInstance.info("Found template for raw image '%s'", imageTag);
            }
            else
            {
                loggerInstance.error("Unable to extract template for raw image '%s'", imageTag);
                return markAsCompleted();
            }
        }

        if (agentLogic.canSupport(DeploymentAgentFeature.DockerBatch, DeploymentAgentFeature.DockerBatchForVolumeCreate, DeploymentAgentFeature.DockerBatchForContainerLaunch))
        {
            return continueAtState(State.CreateBatch);
        }
        else
        {
            return continueAtState(State.CreateScratchVolume);
        }
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_CreateScratchVolume() throws
                                                               Exception
    {
        DeployLogicForAgent agentLogic = getLogicForAgent();
        await(agentLogic.createVolume(scratchVolumeName, containerLabels));

        setAgentStatus(DeploymentStatus.Booting);

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
        ContainerConfiguration config = prepareContainerConfig();

        DeployLogicForAgent agentLogic = getLogicForAgent();
        containerId = await(agentLogic.createContainer("Agent-" + uniqueId, config));

        return continueAtState(State.LoadConfiguration);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_LoadConfiguration() throws
                                                             Exception
    {
        if (!configureThroughEnvVar)
        {
            DeployLogicForAgent agentLogic = getLogicForAgent();
            await(agentLogic.restoreFileSystem(containerId, VOLUME_CONFIG, 2, (file) -> generateConfiguration(file), null));
        }

        // Reschedule to persist state.
        return continueAtState(State.StartContainer);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_StartContainer() throws
                                                          Exception
    {
        DeployLogicForAgent agentLogic = getLogicForAgent();
        await(agentLogic.startContainer(containerId));

        setAgentStatus(DeploymentStatus.Booted);

        // Reschedule to persist state.
        return continueAtState(State.WaitForAgent);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_CreateBatch() throws
                                                       Exception
    {
        List<DockerBatch> list = Lists.newArrayList();

        {
            DockerBatchForVolumeCreate item = new DockerBatchForVolumeCreate();
            item.volumeName = scratchVolumeName;
            item.labels     = containerLabels;
            list.add(item);
        }

        {
            DockerBatchForVolumeCreate item = new DockerBatchForVolumeCreate();
            item.volumeName = configVolumeName;
            item.labels     = containerLabels;
            list.add(item);
        }

        {
            DockerBatchForContainerLaunch item = new DockerBatchForContainerLaunch();
            item.name   = "Agent-" + uniqueId;
            item.config = prepareContainerConfig();

            if (!configureThroughEnvVar)
            {
                DockerBatchForContainerLaunch.FileSystemInit f1 = new DockerBatchForContainerLaunch.FileSystemInit();
                f1.containerPath = VOLUME_CONFIG.toString();
                f1.input         = generateConfiguration("");
                f1.decompress    = true;

                item.configurationFiles = Lists.newArrayList(f1);
            }

            list.add(item);
        }

        DeployLogicForAgent agentLogic = getLogicForAgent();
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

        if (res == null)
        {
            batchToken  = null;
            nextCheckin = 1;
            return continueAtState(State.CreateBatch);
        }

        nextCheckin = 1;
        return continueAtState(State.WaitForAgent);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_WaitForBatch() throws
                                                        Exception
    {
        DeployLogicForAgent agentLogic = getLogicForAgent();
        DockerBatch.Report res = await(agentLogic.checkBatch(batchToken, batchOffset, (line) ->
        {
            loggerInstance.info("%s: %s", getHostDisplayName(), line.payload);

            batchOffset++;
            nextCheckin = 1;
        }));

        reportAgentReachable("Finally able to contact agent");

        if (res == null)
        {
            batchToken  = null;
            nextCheckin = 1;
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

        nextCheckin = 1;
        return continueAtState(State.WaitForAgent);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_WaitForAgent() throws
                                                        Exception
    {
        withLocatorOrNull(loc_targetAgent, (sessionHolder, rec_targetAgent) ->
        {
            if (rec_targetAgent == null)
            {
                markAsFailed("Target agent for '%s' deleted...", getHostDisplayName());
                return;
            }

            if (rec_targetAgent.getRpcId() == null)
            {
                rescheduleDelayed(10, TimeUnit.SECONDS);
                return;
            }

            if (activate)
            {
                final RecordLocked<DeploymentHostRecord> lock_host = getTargetHost(sessionHolder, 2, TimeUnit.MINUTES);
                final DeploymentHostRecord               rec_host  = lock_host.get();

                rec_host.activateAgent(rec_targetAgent);
            }

            markAsCompleted();
        });

        return AsyncRuntime.NullResult;
    }

    @Override
    public void cleanupOnFailure(Throwable t) throws
                                              Exception
    {
        // Don't wait...
        tryToTerminate();
    }

//--//

    private ContainerConfiguration prepareContainerConfig() throws
                                                            Exception
    {
        return withLocatorReadonly(getTargetHostLocator(), (sessionHolder, rec_host) ->
        {
            DeploymentHostFlavor hostFlavor = rec_host.classifyHost(DeploymentHostFlavor.RaspberryPI);

            ContainerConfiguration config = new ContainerConfiguration();
            config.image = imageTag;

            //
            // On ARM32, we map a file that is needed to prevent an automatic reboot.
            //
            if (getArchitecture().isArm32())
            {
                config.addBind(Paths.get("/optio3/watchdog"), Paths.get("/optio3-watchdog"));
                config.addBind(Paths.get("/optio3/heartbeat"), Paths.get("/optio3-heartbeat"));

                config.addBind(Paths.get("/dev"), Paths.get("/optio3-dev"));
                config.addBind(Paths.get("/etc"), Paths.get("/optio3-etc"));
                config.addBind(Paths.get("/var/log"), Paths.get("/optio3-log"));
            }

            config.addBind(configVolumeName, VOLUME_CONFIG);
            config.addBind(scratchVolumeName, VOLUME_SCRATCH);
            config.allowAccessToDockerDaemon = true;
            config.privileged                = true;
            config.networkMode               = "host";
            config.restartAlways             = true;
            config.labels                    = containerLabels;
            config.overrideEntrypoint("sh /app/docker-launch.sh");
            config.overrideCommandLine("deployer /optio3-config/deployer-prod.yml");

            hostFlavor.fixupContainerConfig(rec_host, DeploymentRole.deployer, config);

            int maxMemory = hostFlavor.getMaxHeapMemory(rec_host, DeploymentRole.deployer, 0);
            if (maxMemory > 0)
            {
                WellKnownEnvironmentVariable.MaxMemory.setValue(config.environmentVariables, maxMemory);
            }

            if (configureThroughEnvVar)
            {
                configureThroughEnvVariable(config, () -> generateConfiguration("/optio3-config"));
            }

            return config;
        });
    }

    private void generateConfiguration(File file) throws
                                                  Exception
    {
        FileUtils.writeByteArrayToFile(file, generateConfiguration(""));
    }

    private byte[] generateConfiguration(String root) throws
                                                      Exception
    {
        CommonDeployer.ConfigIdentities identities = new CommonDeployer.ConfigIdentities();
        identities.host.id    = getHostId();
        identities.instanceId = instanceId;

        StubDeployer deployer = new StubDeployer(appConfig.credentials, null, appConfig.getCloudConnectionUrl(), identities);

        if (rawTemplate != null)
        {
            deployer.configTemplate = rawTemplate;
        }
        else
        {
            callInReadOnlySession(sessionHolder ->
                                  {
                                      RegistryTaggedImageRecord rec_taggedImage = null;

                                      if (loc_image != null)
                                      {
                                          rec_taggedImage = sessionHolder.fromLocator(loc_image);
                                      }
                                      else
                                      {
                                          final DeploymentHostRecord rec_host = getTargetHostNoLock(sessionHolder);
                                          DockerImageArchitecture    arch     = rec_host != null ? rec_host.getArchitecture() : DockerImageArchitecture.UNKNOWN;

                                          for (ReleaseStatusReport result : RegistryTaggedImageRecord.reportReleaseStatus(sessionHolder, RegistryImageReleaseStatus.Release))
                                          {
                                              if (result.role == DeploymentRole.deployer && result.architecture == arch)
                                              {
                                                  rec_taggedImage = sessionHolder.fromIdentity(result.image);
                                              }
                                          }

                                          if (rec_taggedImage == null)
                                          {
                                              throw new RuntimeException("Can't find agent image!");
                                          }
                                      }

                                      Base64EncodedValue config = rec_taggedImage.findLabelOrDefault(WellKnownDockerImageLabel.ConfigTemplate, null);
                                      if (config != null)
                                      {
                                          deployer.configTemplate = new String(config.getValue());
                                      }
                                  });
        }

        String url = deployer.connectionUrl;
        url = url.replace("wss://", "https://");
        url = url.replace("ws://", "http://");

        URL    urlParsed = new URL(url);
        String dnsHints  = JsonWebSocketDnsHints.prepareForYAML(urlParsed.getHost());

        return deployer.generateConfiguration(root, dnsHints);
    }

    private CompletableFuture<Void> tryToTerminate() throws
                                                     Exception
    {
        try
        {
            setAgentStatus(DeploymentStatus.Terminating);

            DeployLogicForAgent agentLogic = getLogicForAgent();

            if (containerId != null)
            {
                await(agentLogic.removeContainer(containerId));

                await(agentLogic.deleteVolume(configVolumeName, true));
                await(agentLogic.deleteVolume(scratchVolumeName, true));
            }

            if (batchToken != null)
            {
                await(agentLogic.closeBatch(batchToken));
            }
        }
        catch (TimeoutException t1)
        {
            loggerInstance.error("Caught exception on cleanup: %s", t1.getMessage());
        }
        catch (Throwable t2)
        {
            loggerInstance.error("Caught exception on cleanup: %s", t2);
        }
        finally
        {
            setAgentStatus(DeploymentStatus.Terminated);
        }

        return AsyncRuntime.NullResult;
    }
}
