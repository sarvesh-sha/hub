/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.deploy;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;
import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.optio3.cloud.builder.logic.deploy.DeployLogicForAgent;
import com.optio3.cloud.builder.logic.deploy.DeployLogicForAgentOnHost;
import com.optio3.cloud.builder.logic.deploy.DeployLogicForHub;
import com.optio3.cloud.builder.orchestration.AbstractBuilderActivityHandler;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentAgentRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentTaskRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryTaggedImageRecord;
import com.optio3.cloud.client.deployer.model.BatchToken;
import com.optio3.cloud.client.deployer.model.ContainerConfiguration;
import com.optio3.cloud.client.deployer.model.ContainerStatus;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.cloud.client.deployer.model.MountPointStatus;
import com.optio3.cloud.exception.InvalidStateException;
import com.optio3.cloud.logic.BackgroundActivityHandler;
import com.optio3.cloud.logic.BackgroundActivityStatus;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.concurrency.Executors;
import com.optio3.infra.WellKnownEnvironmentVariable;
import com.optio3.logging.ILogger;
import com.optio3.util.Exceptions;
import com.optio3.util.IdGenerator;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import com.optio3.util.function.ConsumerWithException;

public abstract class BaseDeployTask extends AbstractBuilderActivityHandler
{
    public static class ActivityWithTask
    {
        public RecordLocator<BackgroundActivityRecord> activity;
        public RecordLocator<DeploymentTaskRecord>     task;
    }

    //--//

    public static final Path VOLUME_CONFIG  = Paths.get("/optio3-config");
    public static final Path VOLUME_SCRATCH = Paths.get("/optio3-scratch");
    public static final Path VOLUME_DATA    = Paths.get("/optio3-data");

    //--//

    public String instanceId;
    public String buildId;
    public String imageTag;

    public String uniqueId;
    public String configVolumeName;
    public String scratchVolumeName;
    public String dataVolumeName;

    public RecordLocator<BackgroundActivityRecord> loc_task_imagePull;

    //--//

    protected static <T extends BaseDeployTask> BackgroundActivityRecord scheduleActivity(RecordLocked<DeploymentHostRecord> targetHost,
                                                                                          Duration delay,
                                                                                          Class<T> handler,
                                                                                          ConsumerWithException<T> configure) throws
                                                                                                                              Exception
    {
        requireNonNull(targetHost);

        return scheduleActivity(targetHost, null, (DeploymentRole[]) null, delay, handler, configure);
    }

    protected static <T extends BaseDeployTask> BackgroundActivityRecord scheduleActivity(RecordLocked<DeploymentHostRecord> targetHost,
                                                                                          CustomerServiceRecord targetService,
                                                                                          DeploymentRole targetRole,
                                                                                          Duration delay,
                                                                                          Class<T> handler,
                                                                                          ConsumerWithException<T> configure) throws
                                                                                                                              Exception
    {
        requireNonNull(targetService);
        requireNonNull(targetRole);

        return scheduleActivity(targetHost, targetService, new DeploymentRole[] { targetRole }, delay, handler, configure);
    }

    protected static <T extends BaseDeployTask> BackgroundActivityRecord scheduleActivity(RecordLocked<DeploymentHostRecord> lock_targetHost,
                                                                                          CustomerServiceRecord targetService,
                                                                                          DeploymentRole[] targetRoles,
                                                                                          Duration delay,
                                                                                          Class<T> handlerClass,
                                                                                          ConsumerWithException<T> configure) throws
                                                                                                                              Exception
    {
        SessionHolder        sessionHolder = lock_targetHost.getSessionHolder();
        DeploymentHostRecord targetHost    = lock_targetHost.get();

        CommonDeployState js = new CommonDeployState(sessionHolder, targetService, targetRoles, targetHost);

        //--//

        T newHandler = BackgroundActivityHandler.allocate(handlerClass);
        newHandler.putStateValue("<root>", js);
        newHandler.configureContext();

        newHandler.uniqueId          = IdGenerator.newGuid();
        newHandler.configVolumeName  = "config-" + newHandler.uniqueId;
        newHandler.scratchVolumeName = "scratch-" + newHandler.uniqueId;
        newHandler.dataVolumeName    = "data-" + newHandler.uniqueId;

        if (targetHost != null)
        {
            // Pick a really long timeout for ARM, in case we are connected over Cellular networks.
            newHandler.initializeTimeout(targetHost.isArm32() ? Duration.of(12, ChronoUnit.HOURS) : Duration.of(30, ChronoUnit.MINUTES));
        }

        configure.accept(newHandler);

        ZonedDateTime when = delay != null ? TimeUtils.now()
                                                      .plus(delay) : null;

        return newHandler.schedule(sessionHolder, when);
    }

    protected static <T extends BaseDeployTask> BackgroundActivityRecord scheduleActivity(RecordLocked<CustomerServiceRecord> lock_targetService,
                                                                                          Class<T> handlerClass,
                                                                                          ConsumerWithException<T> configure) throws
                                                                                                                              Exception
    {
        SessionHolder         sessionHolder = lock_targetService.getSessionHolder();
        CustomerServiceRecord targetService = lock_targetService.get();

        CommonDeployState js = new CommonDeployState(sessionHolder, targetService, null, null);

        //--//

        T newHandler = BackgroundActivityHandler.allocate(handlerClass);
        newHandler.putStateValue("<root>", js);
        newHandler.configureContext();

        configure.accept(newHandler);

        return newHandler.schedule(sessionHolder, null);
    }

    //--//

    @Override
    public RecordLocator<? extends RecordWithCommonFields> getContext()
    {
        return getTargetHostLocator();
    }

    protected void reportAgentNotReachable(String fmt,
                                           Object... args)
    {
        if (firstFailedAttempt == null)
        {
            firstFailedAttempt = TimeUtils.now();
            loggerInstance.warn("%s on host '%s'", String.format(fmt, args), getHostDisplayName());

            nextAttempt = 5;
        }

        nextAttempt = Math.min(60, nextAttempt + 5);
    }

    protected void reportAgentReachable(String fmt,
                                        Object... args)
    {
        if (firstFailedAttempt != null)
        {
            Duration elapsed = Duration.between(firstFailedAttempt, TimeUtils.now());
            loggerInstance.warn("%s on host '%s' after %s", String.format(fmt, args), getHostDisplayName(), TimeUtils.toText(elapsed));
            firstFailedAttempt = null;
        }
    }

    //--//

    protected CompletableFuture<Boolean> handlePullImage(RecordLocator<RegistryTaggedImageRecord> loc_taggedImage) throws
                                                                                                                   Exception
    {
        boolean scheduled = withLocator(loc_taggedImage, (sessionHolder, rec_taggedImage) ->
        {
            BackgroundActivityRecord subActivity = sessionHolder.fromLocator(loc_task_imagePull);
            if (subActivity != null)
            {
                if (subActivity.getStatus() != BackgroundActivityStatus.COMPLETED)
                {
                    throw Exceptions.newRuntimeException("Agent '%s' on host '%s' failed to pull image '%s'", instanceId, getHostDisplayName(), rec_taggedImage.getTag());
                }

                loc_task_imagePull = null;
                return false;
            }

            RecordLocked<DeploymentHostRecord> lock_host = getTargetHost(sessionHolder, 2, TimeUnit.MINUTES);

            BackgroundActivityRecord activity = TaskForImagePull.scheduleTask(lock_host, rec_taggedImage, false);
            loc_task_imagePull = sessionHolder.createLocator(activity);

            return true;
        });

        if (scheduled)
        {
            waitForSubActivity(loc_task_imagePull, null);
        }

        return wrapAsync(scheduled);
    }

    //--//

    protected <T> DeployLogicForAgent.TransferProgress<T> createTransferTracker(ILogger logger,
                                                                                String hostId,
                                                                                String fileId)
    {
        return new DeployLogicForAgent.TransferProgress<T>()
        {
            private static final long c_reportStep = 200_000_000;

            private ScheduledFuture<?> m_timeout;
            private ZonedDateTime m_startedOn;
            private long m_nextReport;
            private boolean m_cancelled;
            private MonotonousTime m_nextCancelCheck;

            @Override
            public void notifyBegin()
            {
                logger.info("%s: Transfer of '%s' [%,d bytes]", hostId, fileId, totalSize);

                m_startedOn  = TimeUtils.now();
                m_nextReport = c_reportStep;

                setTimeout();
            }

            @Override
            public void notifyUpdate()
            {
                cancelTimeout();
                setTimeout();

                if (currentPos > m_nextReport)
                {
                    ZonedDateTime now          = TimeUtils.now();
                    Duration      duration     = Duration.between(m_startedOn, now);
                    long          durationSecs = duration.getSeconds();

                    long speed        = currentPos / Math.max(1, durationSecs);
                    long etaInSeconds = (totalSize - currentPos) / speed;

                    if (etaInSeconds > 120)
                    {
                        logger.info("%s: Transfer of '%s' [%,d bytes] at offset %,d... (ETA %d minutes)", hostId, fileId, totalSize, currentPos, (etaInSeconds + 59) / 60);
                    }
                    else
                    {
                        logger.info("%s: Transfer of '%s' [%,d bytes] at offset %,d... (ETA %d seconds)", hostId, fileId, totalSize, currentPos, etaInSeconds);
                    }

                    m_nextReport += c_reportStep;
                }
            }

            @Override
            public void notifyEnd(boolean success)
            {
                cancelTimeout();

                if (success)
                {
                    ZonedDateTime endedAt      = TimeUtils.now();
                    Duration      duration     = Duration.between(m_startedOn, endedAt);
                    long          durationSecs = duration.getSeconds();

                    logger.info("%s: Transfer of '%s' [%,d bytes] completed (%,d bytes/sec)", hostId, fileId, totalSize, totalSize / (Math.max(1, durationSecs)));
                }
                else
                {
                    logger.info("%s: Transfer of '%s' [%,d bytes] failed at offset %,d", hostId, fileId, totalSize, currentPos);
                }
            }

            @Override
            public boolean wasCancelled()
            {
                if (!m_cancelled)
                {
                    if (TimeUtils.isTimeoutExpired(m_nextCancelCheck))
                    {
                        m_cancelled       = BaseDeployTask.this.shouldStopProcessing();
                        m_nextCancelCheck = TimeUtils.computeTimeoutExpiration(30, TimeUnit.SECONDS);
                    }
                }

                return m_cancelled;
            }

            //--//

            private void cancelTimeout()
            {
                ScheduledFuture<?> timeout = m_timeout;
                m_timeout = null;

                if (timeout != null)
                {
                    timeout.cancel(false);
                }
            }

            private void setTimeout()
            {
                m_timeout = Executors.scheduleOnDefaultPool(this::triggerTimeout, 10, TimeUnit.SECONDS);
            }

            private void triggerTimeout()
            {
                if (m_timeout != null)
                {
                    logger.warn("Transfer of %,d bytes stalled at offset %,d...", totalSize, currentPos);
                }
            }
        };
    }

    //--//

    protected static ActivityWithTask trackActivityWithTask(RecordLocked<DeploymentHostRecord> lock_targetHost,
                                                            BackgroundActivityRecord rec_activity,
                                                            RegistryTaggedImageRecord image,
                                                            Map<String, String> containerLabels)
    {
        return trackActivityWithTask(lock_targetHost, rec_activity, image.getTag(), containerLabels);
    }

    protected static ActivityWithTask trackActivityWithTask(RecordLocked<DeploymentHostRecord> lock_targetHost,
                                                            BackgroundActivityRecord rec_activity,
                                                            String imageTag,
                                                            Map<String, String> containerLabels)
    {
        SessionHolder sessionHolder = lock_targetHost.getSessionHolder();

        //
        // Create Task record, so we can track its progress.
        //
        DeploymentTaskRecord rec_task = DeploymentTaskRecord.newInstance(lock_targetHost.get());
        rec_task.setImage(imageTag);
        rec_task.updateLabels(containerLabels);

        sessionHolder.persistEntity(rec_task);

        ActivityWithTask res = new ActivityWithTask();
        res.activity = sessionHolder.createLocator(rec_activity);
        res.task     = sessionHolder.createLocator(rec_task);
        return res;
    }

    //--//

    protected CompletableFuture<Void> tryToTerminate(BatchToken batchToken,
                                                     String containerId,
                                                     boolean deleteVolumes)
    {
        try
        {
            DeployLogicForAgent agentLogic = getLogicForAgent();

            if (containerId != null)
            {
                await(agentLogic.removeContainer(containerId));

                if (deleteVolumes)
                {
                    await(agentLogic.deleteVolume(dataVolumeName, true));
                    await(agentLogic.deleteVolume(configVolumeName, true));
                    await(agentLogic.deleteVolume(scratchVolumeName, true));
                }
            }

            if (batchToken != null)
            {
                await(agentLogic.closeBatch(batchToken));
            }
        }
        catch (Throwable t)
        {
            loggerInstance.error("Caught exception on cleanup: %s", t);
        }

        return wrapAsync(null);
    }

    //--//

    protected final DeployLogicForAgent getLogicForAgent() throws
                                                           Exception
    {
        return withLocatorReadonly(getTargetHostLocator(), DeployLogicForAgent::new);
    }

    protected final DeployLogicForAgentOnHost getLogicForAgentOnHost() throws
                                                                       Exception
    {
        return withLocatorReadonly(getTargetHostLocator(), (sessionHolder, rec_host) ->
        {
            return new DeployLogicForAgentOnHost(sessionHolder, rec_host, getTargetService(sessionHolder), getTargetRoles());
        });
    }

    protected final DeployLogicForAgentOnHost getLogicForAgentOnHostOrNull() throws
                                                                             Exception
    {
        return withLocatorReadonlyOrNull(getTargetHostLocator(), (sessionHolder, rec_host) ->
        {
            return rec_host != null ? new DeployLogicForAgentOnHost(sessionHolder, rec_host, getTargetService(sessionHolder), getTargetRoles()) : null;
        });
    }

    protected final DeployLogicForHub getLogicForHub() throws
                                                       Exception
    {
        return DeployLogicForHub.fromLocator(getSessionProvider(), getTargetServiceLocator());
    }

    protected DockerImageArchitecture getArchitecture() throws
                                                        Exception
    {
        return withLocatorReadonlyOrNull(getTargetHostLocator(), (sessionHolder, rec_host) ->
        {
            return rec_host != null ? rec_host.getArchitecture() : DockerImageArchitecture.UNKNOWN;
        });
    }

    private CommonDeployState getCommonStateRaw()
    {
        return getStateValue(CommonDeployState.class, "<root>");
    }

    private CommonDeployState getCommonState()
    {
        return getCommonStateRaw();
    }

    protected DeploymentAgentRecord getActiveAgent(SessionHolder sessionHolder)
    {
        final DeploymentHostRecord rec_host = getTargetHostNoLock(sessionHolder);

        return rec_host != null ? rec_host.findActiveAgent() : null;
    }

    protected DeploymentAgentRecord ensureActiveAgent(SessionHolder sessionHolder) throws
                                                                                   Exception
    {
        DeploymentAgentRecord rec_activeAgent = getActiveAgent(sessionHolder);
        Exceptions.requireNotNull(rec_activeAgent, InvalidStateException.class, "No active agent on host '%s'", getHostDisplayName());

        return rec_activeAgent;
    }

    protected final RecordLocator<CustomerServiceRecord> getTargetServiceLocator()
    {
        return getCommonState().locatorForTargetService;
    }

    protected final RecordLocator<DeploymentHostRecord> getTargetHostLocator()
    {
        return getCommonState().locatorForTargetHost;
    }

    protected final CustomerServiceRecord getTargetService(SessionHolder sessionHolder)
    {
        return sessionHolder.fromLocator(getTargetServiceLocator());
    }

    protected final RecordLocked<CustomerServiceRecord> getTargetServiceLocked(SessionHolder sessionHolder,
                                                                               long timeout,
                                                                               TimeUnit unit)
    {
        return sessionHolder.fromLocatorWithLock(getTargetServiceLocator(), timeout, unit);
    }

    protected final DeploymentRole[] getTargetRoles()
    {
        return getCommonState().targetRoles;
    }

    protected final RecordLocked<DeploymentHostRecord> getTargetHost(SessionHolder sessionHolder,
                                                                     long timeout,
                                                                     TimeUnit unit)
    {
        return sessionHolder.fromLocatorWithLock(getTargetHostLocator(), timeout, unit);
    }

    protected final DeploymentHostRecord getTargetHostNoLock(SessionHolder sessionHolder)
    {
        return sessionHolder.fromLocator(getTargetHostLocator());
    }

    protected final RecordLocked<DeploymentHostRecord> getTargetHostOrNull(SessionHolder sessionHolder,
                                                                           long timeout,
                                                                           TimeUnit unit)
    {
        return sessionHolder.fromLocatorWithLockOrNull(getTargetHostLocator(), timeout, unit);
    }

    protected final DeploymentHostRecord getTargetHostOrNullNoLock(SessionHolder sessionHolder)
    {
        return sessionHolder.fromLocatorOrNull(getTargetHostLocator());
    }

    public boolean sameTargetHost(RecordLocator<DeploymentHostRecord> loc)
    {
        return loc != null && loc.equals(getCommonState().locatorForTargetHost);
    }

    protected final String getHostId()
    {
        return getCommonState().hostId;
    }

    protected String getHostDisplayName()
    {
        return getCommonState().hostDisplayName;
    }

    //--//

    protected void configureThroughEnvVariable(ContainerConfiguration config,
                                               Callable<byte[]> generatorCallback) throws
                                                                                   Exception
    {
        WellKnownEnvironmentVariable.FileSystemPatch.setValue(config.environmentVariables, generatorCallback.call());
    }

    protected CompletableFuture<Void> removeContainerAndVolumes(DeployLogicForAgent agentLogic,
                                                                String containerId) throws
                                                                                    Exception
    {
        ContainerStatus agentStatus = await(agentLogic.removeContainer(containerId));
        if (agentStatus != null)
        {
            for (MountPointStatus mp : agentStatus.mountPoints)
            {
                if (mp.isVolume())
                {
                    await(agentLogic.deleteVolume(mp.name, true));
                }
            }
        }

        return wrapAsync(null);
    }

    //--//

    protected void joinNetworkIfNeeded(ContainerConfiguration config,
                                       boolean addAlias) throws
                                                         Exception
    {
        callInReadOnlySession(sessionHolder -> joinNetworkIfNeeded(sessionHolder, config, addAlias));
    }

    protected void joinNetworkIfNeeded(SessionHolder sessionHolder,
                                       ContainerConfiguration config,
                                       boolean addAlias)
    {
        CustomerServiceRecord rec_svc = getTargetService(sessionHolder);
        switch (rec_svc.getDbMode())
        {
            case MariaDB:
                config.networkMode = "Network-" + rec_svc.getSysId();

                if (addAlias)
                {
                    config.networkAlias = rec_svc.getDbConfiguration()
                                                 .getServerName();
                }
                break;
        }
    }
}
