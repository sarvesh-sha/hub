/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.deploy;

import static com.optio3.asyncawait.CompileTime.await;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.archive.TarBuilder;
import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.JsonWebSocketDnsHints;
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.logic.deploy.BaseDeployLogic;
import com.optio3.cloud.builder.logic.deploy.DeployLogicForAgent;
import com.optio3.cloud.builder.model.deployment.DeploymentHostFlavor;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentAgentRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentTaskRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryTaggedImageRecord;
import com.optio3.cloud.client.deployer.model.ContainerConfiguration;
import com.optio3.cloud.client.deployer.model.ContainerStatus;
import com.optio3.cloud.client.deployer.model.DeploymentAgentFeature;
import com.optio3.cloud.client.deployer.model.batch.DockerBatch;
import com.optio3.cloud.client.deployer.model.batch.DockerBatchForContainerLaunch;
import com.optio3.cloud.client.deployer.model.batch.DockerBatchForContainerTerminate;
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
import com.optio3.util.Base64EncodedValue;
import com.optio3.util.CollectionUtils;
import com.optio3.util.ConfigVariables;
import com.optio3.util.Encryption;
import com.optio3.util.Exceptions;
import com.optio3.util.IConfigVariable;
import com.optio3.util.IdGenerator;
import org.apache.commons.io.FileUtils;

public class TaskForGatewayCreation extends BaseDeployTaskCreation implements BackgroundActivityHandler.ICleanupOnFailure
{
    public enum State
    {
        PullImage,
        TaskTermination,
        CreateScratchVolume,
        CreateConfigVolume,
        CreateDataVolume,
        CreateContainer,
        LoadConfiguration,
        StartContainer,
        //--//
        CreateBatch,
        StartBatch,
        WaitForBatch
    }

    public enum ConfigVariable implements IConfigVariable
    {
        DnsHints("DNS_HINTS"),
        WebSocketConnectionUrl("WS_CONNECTION_URL"),
        AccountName("ACCOUNT_NAME"),
        AccountPassword("ACCOUNT_PASSWORD"),
        InstanceId("INSTANCE_ID");

        private final String m_variable;

        ConfigVariable(String variable)
        {
            m_variable = variable;
        }

        public String getVariable()
        {
            return m_variable;
        }
    }

    private static final ConfigVariables.Validator<ConfigVariable> s_configValidator        = new ConfigVariables.Validator<>(ConfigVariable.values());
    private static final ConfigVariables.Template<ConfigVariable>  s_template_YamlFromBuild = s_configValidator.newTemplate(TaskForGatewayCreation.class, null, "${", "}");

    //--//

    public static ActivityWithTask scheduleTask(RecordLocked<DeploymentHostRecord> lock_targetHost,
                                                List<RecordLocator<DeploymentTaskRecord>> loc_tasksToStop,
                                                RegistryTaggedImageRecord image) throws
                                                                                 Exception
    {
        Exceptions.requireNotNull(lock_targetHost, InvalidArgumentException.class, "No host provided");

        Exceptions.requireNotNull(image.findLabelOrDefault(WellKnownDockerImageLabel.ConfigTemplate, null), InvalidArgumentException.class, "No config template in image %s", image.getTag());

        SessionHolder         sessionHolder   = lock_targetHost.getSessionHolder();
        DeploymentHostRecord  rec_targetHost  = lock_targetHost.get();
        String                hostId          = rec_targetHost.getHostId();
        DeploymentAgentRecord rec_agentActive = rec_targetHost.findActiveAgent();
        Exceptions.requireNotNull(rec_agentActive, InvalidStateException.class, "No active agent on host '%s'", hostId);

        DeploymentRole role = rec_targetHost.findRoleCompatibleWithImage(image);
        Exceptions.requireNotNull(role, InvalidStateException.class, "No role associated with host '%s'", hostId);

        CustomerServiceRecord rec_svc = rec_targetHost.getCustomerService();

        //
        // Create automation password for this Hub.
        //
        if (rec_svc.getMaintPassword() == null)
        {
            BuilderConfiguration cfg = sessionHolder.getServiceNonNull(BuilderConfiguration.class);

            rec_svc.setMaintPassword(cfg.encrypt(Encryption.generateRandomKeyAsBase64()));
        }

        Map<String, String> containerLabels = Maps.newHashMap();

        BackgroundActivityRecord rec_activity = BaseDeployTask.scheduleActivity(lock_targetHost, rec_svc, role, null, TaskForGatewayCreation.class, (t) ->
        {
            t.loggerInstance.info("Provisioning Gateway on host '%s' with image '%s'", hostId, image.getTag());

            if (loc_tasksToStop != null)
            {
                t.loc_tasksToStop.addAll(loc_tasksToStop);
            }

            t.loc_image = sessionHolder.createLocator(image);

            t.instanceId = t.uniqueId;
            t.imageTag   = image.getTag();

            t.containerLabels = containerLabels;
            WellKnownDockerImageLabel.DeploymentPurpose.setValue(t.containerLabels, DeploymentRole.gateway.name());
            WellKnownDockerImageLabel.DeploymentContextId.setValue(t.containerLabels, hostId);
            WellKnownDockerImageLabel.DeploymentInstanceId.setValue(t.containerLabels, t.instanceId);
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
        return String.format("Create Gateway '%s' on host '%s' using image '%s'", instanceId, getHostDisplayName(), imageTag);
    }

    @BackgroundActivityMethod(stateClass = State.class, initial = true, autoRetry = true)
    public CompletableFuture<Void> state_PullImage() throws
                                                     Exception
    {
        if (await(handlePullImage(loc_image)))
        {
            return AsyncRuntime.NullResult;
        }

        boolean done = withLocatorReadonly(loc_image, (sessionHolder, rec_taggedImage) ->
        {
            DeploymentHostRecord rec_targetHost = getTargetHostNoLock(sessionHolder);
            for (DeploymentTaskRecord rec_task : rec_targetHost.findTasksForPurpose(null, null, rec_taggedImage, true))
            {
                if (CollectionUtils.findFirst(loc_tasksToStop, (loc_task) -> loc_task.sameRecord(rec_task)) != null)
                {
                    continue;
                }

                loggerInstance.warn("Task running image '%s' already present on host '%s', exiting...", rec_taggedImage.getTag(), rec_targetHost.getDisplayName());

                return true;
            }

            return false;
        });

        if (done)
        {
            return markAsCompleted();
        }

        DeployLogicForAgent agentLogic = getLogicForAgent();
        if (agentLogic.canSupport(DeploymentAgentFeature.DockerBatch, DeploymentAgentFeature.DockerBatchForVolumeCreate, DeploymentAgentFeature.DockerBatchForContainerLaunch))
        {
            return continueAtState(State.CreateBatch);
        }
        else
        {
            return continueAtState(State.TaskTermination);
        }
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_TaskTermination() throws
                                                           Exception
    {
        await(terminateOldTasks());

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

        // Reschedule to persist state.
        return continueAtState(State.CreateDataVolume);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_CreateDataVolume() throws
                                                            Exception
    {
        DeployLogicForAgent agentLogic = getLogicForAgent();
        await(agentLogic.createVolume(dataVolumeName, containerLabels));

        // Reschedule to persist state.
        return continueAtState(State.CreateContainer);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_CreateContainer() throws
                                                           Exception
    {
        ContainerConfiguration config = prepareContainerConfig();

        DeployLogicForAgent agentLogic = getLogicForAgent();
        containerId = await(agentLogic.createContainer("Gateway-" + uniqueId, config));

        return continueAtState(State.LoadConfiguration);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_LoadConfiguration() throws
                                                             Exception
    {
        if (!configureThroughEnvVar)
        {
            DeployLogicForAgent agentLogic = getLogicForAgent();
            await(agentLogic.restoreFileSystem(containerId, VOLUME_CONFIG, 2, file -> generateConfiguration(file), null));
        }

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

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_CreateBatch() throws
                                                       Exception
    {
        DeployLogicForAgent agentLogic = getLogicForAgent();

        List<DockerBatch> list = Lists.newArrayList();

        callInReadOnlySession(sessionHolder ->
                              {
                                  for (RecordLocator<DeploymentTaskRecord> loc_task : loc_tasksToStop)
                                  {
                                      DeploymentTaskRecord rec_task = sessionHolder.fromLocatorOrNull(loc_task);
                                      if (rec_task != null)
                                      {
                                          DockerBatchForContainerTerminate item = new DockerBatchForContainerTerminate();
                                          item.dockerId = rec_task.getDockerId();
                                          list.add(item);
                                      }
                                  }
                              });

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
            DockerBatchForVolumeCreate item = new DockerBatchForVolumeCreate();
            item.volumeName = dataVolumeName;
            item.labels     = containerLabels;
            list.add(item);
        }

        {
            DockerBatchForContainerLaunch item = new DockerBatchForContainerLaunch();
            item.name   = "Gateway-" + uniqueId;
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
            loggerInstance.info("%s: %s", getHostDisplayName(), line.payload);
            batchOffset++;
            nextCheckin = 1;
        }));

        reportAgentReachable("Finally able to contact agent");

        if (res == null)
        {
            batchToken  = null;
            nextCheckin = 1;
            return continueAtState(State.CreateBatch, 10, TimeUnit.SECONDS);
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
                // Force termination of partially created containers.
                for (ContainerStatus container : await(agentLogic.listContainers()))
                {
                    String         purposeText = WellKnownDockerImageLabel.DeploymentPurpose.getValue(container.labels);
                    DeploymentRole purpose     = DeploymentRole.parse(purposeText);
                    if (purpose == DeploymentRole.gateway)
                    {
                        await(tryToTerminate(null, container.id, true));
                    }
                }

                loggerInstance.error("Batch for agent failed on host '%s': %s", getHostDisplayName(), result.failure);
                return markAsFailed(result.failure);
            }
        }

        await(agentLogic.closeBatch(batchToken));

        lockedWithLocator(getTargetHostLocator(), 2, TimeUnit.MINUTES, (sessionHolder, lock_host) ->
        {
            DeploymentHostRecord rec_host = lock_host.get();

            // Reset remote details.
            rec_host.setRemoteDetails(null);
        });

        return markAsCompleted();
    }

    @Override
    public void cleanupOnFailure(Throwable t)
    {
        // Don't wait for it...
        tryToTerminate(batchToken, containerId, true);
        batchToken  = null;
        containerId = null;
    }

    //--//

    private ContainerConfiguration prepareContainerConfig() throws
                                                            Exception
    {
        ContainerConfiguration config = new ContainerConfiguration();
        config.image = imageTag;

        //
        // On ARM32, we map the /dev directory, in case the gateway needs raw access to hardware.
        //
        if (getArchitecture().isArm32())
        {
            config.addBind(Paths.get("/dev"), Paths.get("/optio3-dev"));
        }

        config.addBind(configVolumeName, VOLUME_CONFIG);
        config.addBind(scratchVolumeName, VOLUME_SCRATCH);
        config.addBind(dataVolumeName, VOLUME_DATA);
        config.privileged    = true;
        config.networkMode   = "host";
        config.restartAlways = true;
        config.labels        = containerLabels;

        config.overrideEntrypoint("sh /app/docker-launch.sh");
        config.overrideCommandLine("gateway /optio3-config/gateway-prod.yml");

        // Configure max size of Java Heap to 600MB.
        configureMaxMemory(config);

        if (configureThroughEnvVar)
        {
            configureThroughEnvVariable(config, () -> generateConfiguration("/optio3-config"));
        }

        return config;
    }

    public static ContainerConfiguration prepareContainerConfigForOfflineDeployment(BuilderConfiguration appConfig,
                                                                                    CustomerServiceRecord rec_svc,
                                                                                    DeploymentHostRecord rec_host,
                                                                                    RegistryTaggedImageRecord rec_taggedImage) throws
                                                                                                                               Exception
    {
        ContainerConfiguration config = new ContainerConfiguration();
        config.image = rec_taggedImage.getTag();

        //
        // On ARM32, we map the /dev directory, in case the gateway needs raw access to hardware.
        //
        if (rec_host.isArm32())
        {
            config.addBind(Paths.get("/dev"), Paths.get("/optio3-dev"));
        }

        config.privileged    = true;
        config.networkMode   = "host";
        config.restartAlways = true;
        config.labels        = Maps.newHashMap();
        WellKnownDockerImageLabel.DeploymentPurpose.setValue(config.labels, DeploymentRole.gateway.name());
        WellKnownDockerImageLabel.DeploymentContextId.setValue(config.labels, rec_host.getHostId());
        WellKnownDockerImageLabel.DeploymentInstanceId.setValue(config.labels, IdGenerator.newGuid());

        config.overrideEntrypoint("sh /app/docker-launch.sh");
        config.overrideCommandLine("gateway /optio3-config/gateway-prod.yml");

        configureMaxMemory(rec_host, config);

        WellKnownEnvironmentVariable.FileSystemPatch.setValue(config.environmentVariables, generateConfiguration(appConfig, "/optio3-config", rec_svc, rec_host, rec_taggedImage));

        return config;
    }

    private void configureMaxMemory(ContainerConfiguration config) throws
                                                                   Exception
    {
        withLocatorReadonly(getTargetHostLocator(), (sessionHolder, rec_host) ->
        {
            configureMaxMemory(rec_host, config);
        });
    }

    private static void configureMaxMemory(DeploymentHostRecord rec_host,
                                           ContainerConfiguration config) throws
                                                                          Exception
    {
        DeploymentHostFlavor hostFlavor = rec_host.classifyHost(DeploymentHostFlavor.RaspberryPI);

        int maxMemory = hostFlavor.getMaxHeapMemory(rec_host, DeploymentRole.gateway, 0);
        if (maxMemory > 0)
        {
            WellKnownEnvironmentVariable.MaxMemory.setValue(config.environmentVariables, maxMemory);
        }
    }

    private void generateConfiguration(File file) throws
                                                  Exception
    {
        FileUtils.writeByteArrayToFile(file, generateConfiguration(""));
    }

    private byte[] generateConfiguration(String root) throws
                                                      Exception
    {
        return computeInReadOnlySession(sessionHolder -> generateConfiguration(appConfig,
                                                                               root,
                                                                               getTargetService(sessionHolder),
                                                                               getTargetHostNoLock(sessionHolder),
                                                                               sessionHolder.fromLocator(loc_image)));
    }

    public static byte[] generateConfiguration(BuilderConfiguration appConfig,
                                               String root,
                                               CustomerServiceRecord rec_svc,
                                               DeploymentHostRecord rec_host,
                                               RegistryTaggedImageRecord rec_taggedImage) throws
                                                                                          Exception
    {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        try (TarBuilder builder = new TarBuilder(stream, true))
        {
            String url       = rec_svc.getUrl();
            URL    urlParsed = new URL(url);

            url = url.replace("https://", "wss://");
            url = url.replace("http://", "ws://");

            ConfigVariables<ConfigVariable> parameters = s_template_YamlFromBuild.allocate();

            parameters.setValue(ConfigVariable.DnsHints, JsonWebSocketDnsHints.prepareForYAML(urlParsed.getHost()));
            parameters.setValue(ConfigVariable.WebSocketConnectionUrl, url);
            parameters.setValue(ConfigVariable.InstanceId, rec_host.getHostId()); // We use the HostId for instance, since it's stable.

            // Legacy fields, remove after all the gateways have been updated.
            parameters.setValue(ConfigVariable.AccountName, BaseDeployLogic.MACHINE_ACCOUNT);
            parameters.setValue(ConfigVariable.AccountPassword, appConfig.decrypt(rec_svc.getMaintPassword()));

            Base64EncodedValue config = rec_taggedImage.findLabelOrDefault(WellKnownDockerImageLabel.ConfigTemplate, null);
            String             input  = new String(config.getValue());

            String yamlFile = parameters.convert(input);

            builder.addAsString(root, "gateway-prod.yml", yamlFile, 0440);
        }

        return stream.toByteArray();
    }
}
