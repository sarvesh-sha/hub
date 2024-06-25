/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.deploy;

import static com.optio3.asyncawait.CompileTime.await;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.Maps;
import com.optio3.archive.TarBuilder;
import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.builder.logic.deploy.DeployLogicForAgent;
import com.optio3.cloud.builder.logic.deploy.DeployLogicForHub;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentAgentRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentTaskRecord;
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
import com.optio3.infra.directory.CertificateInfo;
import com.optio3.util.Base64EncodedValue;
import com.optio3.util.ConfigVariables;
import com.optio3.util.Exceptions;
import com.optio3.util.IConfigVariable;

public class TaskForReporterCreation extends BaseDeployTaskCreation implements BackgroundActivityHandler.ICleanupOnFailure
{
    public enum State
    {
        PullImage,
        TaskTermination,
        CreateConfigVolume,
        CreateContainer,
        LoadConfiguration,
        StartContainer
    }

    public enum ConfigVariable implements IConfigVariable
    {
        KeyPath("KEY_PATH"),
        CertPath("CERT_PATH");

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
    private static final ConfigVariables.Template<ConfigVariable>  s_template_YamlFromBuild = s_configValidator.newTemplate(TaskForReporterCreation.class, null, "${", "}");

    //--//

    public static ActivityWithTask scheduleTask(RecordLocked<DeploymentHostRecord> lock_targetHost,
                                                List<RecordLocator<DeploymentTaskRecord>> loc_tasksToStop,
                                                RegistryTaggedImageRecord image) throws
                                                                                 Exception
    {
        Exceptions.requireNotNull(lock_targetHost, InvalidArgumentException.class, "No host provided");

        Exceptions.requireNotNull(image.findLabelOrDefault(WellKnownDockerImageLabel.ConfigTemplate, null), InvalidArgumentException.class, "No config template in image %s", image.getTag());

        SessionHolder        sessionHolder  = lock_targetHost.getSessionHolder();
        DeploymentHostRecord rec_targetHost = lock_targetHost.get();

        String                hostId          = rec_targetHost.getHostId();
        DeploymentAgentRecord rec_agentActive = rec_targetHost.findActiveAgent();
        if (rec_agentActive == null)
        {
            throw Exceptions.newRuntimeException("No active agent on host '%s'", hostId);
        }

        DeploymentRole role = rec_targetHost.findRoleCompatibleWithImage(image);
        Exceptions.requireTrue(role == DeploymentRole.reporter, InvalidStateException.class, "No reporter role associated with host '%s'", hostId);

        Map<String, String> containerLabels = Maps.newHashMap();

        BackgroundActivityRecord rec_activity = BaseDeployTask.scheduleActivity(lock_targetHost,
                                                                                rec_targetHost.getCustomerService(),
                                                                                DeploymentRole.reporter,
                                                                                null,
                                                                                TaskForReporterCreation.class,
                                                                                (t) ->
                                                                                {
                                                                                    t.loggerInstance.info("Provisioning Reporter on host '%s' with image '%s'", hostId, image.getTag());

                                                                                    if (loc_tasksToStop != null)
                                                                                    {
                                                                                        t.loc_tasksToStop.addAll(loc_tasksToStop);
                                                                                    }

                                                                                    t.loc_image = sessionHolder.createLocator(image);

                                                                                    t.imageTag = image.getTag();

                                                                                    t.containerLabels = containerLabels;
                                                                                    WellKnownDockerImageLabel.DeploymentPurpose.setValue(t.containerLabels, DeploymentRole.reporter.name());
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
        return String.format("Create Reporter on host '%s' using image '%s'", getHostDisplayName(), imageTag);
    }

    @BackgroundActivityMethod(stateClass = State.class, initial = true, autoRetry = true)
    public CompletableFuture<Void> state_PullImage() throws
                                                     Exception
    {
        if (await(handlePullImage(loc_image)))
        {
            return AsyncRuntime.NullResult;
        }

        return continueAtState(State.TaskTermination);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_TaskTermination() throws
                                                           Exception
    {
        await(terminateOldTasks());

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
        DeployLogicForHub hubLogic = getLogicForHub();

        ContainerConfiguration config = new ContainerConfiguration();
        config.image = imageTag;

        config.addBind(configVolumeName, VOLUME_CONFIG);

        config.addPort(hubLogic.getServicePort(), 8443, false);
        config.labels = containerLabels;
        config.overrideCommandLine("npm run serve:prd");

        DeployLogicForAgent agentLogic = getLogicForAgent();
        containerId = await(agentLogic.createContainer("Reporter-" + uniqueId, config));

        return continueAtState(State.LoadConfiguration);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_LoadConfiguration() throws
                                                             Exception
    {
        DeployLogicForAgent agentLogic = getLogicForAgent();
        await(agentLogic.restoreFileSystem(containerId, VOLUME_CONFIG, 2, this::generateConfiguration, null));

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
        // Don't wait...
        tryToTerminate(null, containerId, true);
    }

    //--//

    private void generateConfiguration(File file) throws
                                                  Exception
    {
        try (FileOutputStream stream = new FileOutputStream(file))
        {
            try (TarBuilder builder = new TarBuilder(stream, true))
            {
                withLocatorReadonly(getTargetServiceLocator(), (sessionHolder, rec_svc) ->
                {
                    CertificateInfo certObj = appConfig.credentials.findCertificate(rec_svc.getUrl());

                    ConfigVariables<ConfigVariable> parameters = s_template_YamlFromBuild.allocate();

                    parameters.setValue(ConfigVariable.KeyPath, builder.addAsBytes("/ssl", "key.pem", certObj.readAndDecryptPrivateFile(), 0444));
                    parameters.setValue(ConfigVariable.CertPath, builder.addAsBytes("/ssl", "cert.pem", certObj.readPublicFileAndCertificateChain(), 0444));

                    RegistryTaggedImageRecord rec_taggedImage = sessionHolder.fromLocator(loc_image);
                    Base64EncodedValue        config          = rec_taggedImage.findLabelOrDefault(WellKnownDockerImageLabel.ConfigTemplate, null);
                    String                    input           = new String(config.getValue());

                    String yamlFile = parameters.convert(input);

                    builder.addAsString(null, "reporter-prod.yml", yamlFile, 0444);
                });
            }
        }
    }
}
