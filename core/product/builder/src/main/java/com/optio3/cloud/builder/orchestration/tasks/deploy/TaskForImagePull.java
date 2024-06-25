/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.deploy;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.sleep;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.NotFoundException;

import com.google.common.collect.Lists;
import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.builder.BuilderApplication;
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.logic.deploy.DeployLogicForAgent;
import com.optio3.cloud.builder.model.deployment.DeploymentHostImage;
import com.optio3.cloud.builder.model.jobs.JobStatus;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.builder.persistence.FixupProcessingRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostImagePullRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryTaggedImageRecord;
import com.optio3.cloud.client.deployer.model.DeploymentAgentFeature;
import com.optio3.cloud.client.deployer.model.ImagePullToken;
import com.optio3.cloud.client.deployer.model.PullProgress;
import com.optio3.cloud.client.deployer.model.PullProgressStatus;
import com.optio3.cloud.client.deployer.model.ShellOutput;
import com.optio3.cloud.client.deployer.proxy.DeployerDockerApi;
import com.optio3.cloud.exception.InvalidArgumentException;
import com.optio3.cloud.logic.BackgroundActivityHandler;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.persistence.LogEntry;
import com.optio3.cloud.persistence.LogHolder;
import com.optio3.cloud.persistence.RecordForFixupProcessing;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.concurrency.Executors;
import com.optio3.infra.WellKnownDockerImageLabel;
import com.optio3.infra.WellKnownSites;
import com.optio3.infra.directory.RoleType;
import com.optio3.infra.directory.UserInfo;
import com.optio3.infra.docker.DockerImageDownloader;
import com.optio3.infra.docker.DockerImageIdentifier;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.util.ConfigVariables;
import com.optio3.util.Exceptions;
import com.optio3.util.IConfigVariable;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

public class TaskForImagePull extends BaseDeployTask implements BackgroundActivityHandler.ICleanupOnComplete,
                                                                BackgroundActivityHandler.ICleanupOnFailure
{
    // To ensure that pending pull will continue, we need to analyze all the images currently being downloaded.
    public static class FixupForPendingPulls extends FixupProcessingRecord.Handler
    {
        @Override
        public RecordForFixupProcessing.Handler.Result process(Logger logger,
                                                               SessionHolder sessionHolder) throws
                                                                                            Exception
        {
            DockerImageDownloader dl   = sessionHolder.getServiceNonNull(DockerImageDownloader.class);
            BuilderConfiguration  cfg  = sessionHolder.getServiceNonNull(BuilderConfiguration.class);
            UserInfo              user = cfg.credentials != null ? cfg.credentials.findFirstAutomationUser(WellKnownSites.dockerRegistry(), RoleType.Subscriber) : null;

            for (var ri : BackgroundActivityRecord.findHandlers(sessionHolder, false, true, TaskForImagePull.class, null))
            {
                BackgroundActivityRecord rec     = sessionHolder.fromIdentity(ri);
                TaskForImagePull         handler = (TaskForImagePull) rec.getHandler(sessionHolder);

                if (handler.imageTag != null)
                {
                    DockerImageIdentifier img = new DockerImageIdentifier(handler.imageTag);

                    if (cfg.developerSettings.developerMode)
                    {
                        //
                        // When running in a Developer environment, perform the analysis in the background.
                        //
                        Executors.scheduleOnDefaultPool(() -> performAnalysis(logger, dl, user, img), 0, TimeUnit.SECONDS);
                    }
                    else
                    {
                        //
                        // When running in a production environment, perform the analysis in the foreground,
                        // to prevent chunk download requests from failed due to missing summary.
                        //
                        performAnalysis(logger, dl, user, img);
                    }
                }
            }

            return Result.RunAgainAtBoot;
        }

        private static void performAnalysis(Logger logger,
                                            DockerImageDownloader dl,
                                            UserInfo user,
                                            DockerImageIdentifier img)
        {
            try (DockerImageDownloader.Reservation reservation = dl.acquire()
                                                                   .get())
            {
                DockerImageDownloader.PackageOfImages pkgFound = reservation.analyze(user, img);
                pkgFound.processAllChunks();
            }
            catch (Throwable e)
            {
                logger.error("Parsing image '%s' failed, due to %s", img.fullName, e);
            }
        }
    }

    enum ConfigVariable implements IConfigVariable
    {
        HostSysId("HOST_SYSID"),
        HostId("HOST_ID"),
        Image("IMAGE"),
        Attempts("ATTEMPTS");

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

    private static final ConfigVariables.Validator<ConfigVariable> s_configValidator = new ConfigVariables.Validator<>(ConfigVariable.values());
    private static final ConfigVariables.Template<ConfigVariable>  s_template_stuck  = s_configValidator.newTemplate(TaskForImagePull.class, "emails/downloads/pull_stuck_in_a_loop.txt", "${", "}");

    //--//

    public enum State
    {
        CheckImageLocally,
        StartingPull,
        CheckingPull,
        TaggingImage
    }

    public RecordLocator<RegistryTaggedImageRecord>     loc_image;
    public int                                          retries;
    public ImagePullToken                               pullToken;
    public int                                          pullOffset;
    public int                                          pullRounds;
    public int                                          nextCheck;
    public int                                          minimumCheckDelay;
    public Duration                                     extraTime;
    public String                                       imageTagTarget;
    public RecordLocator<DeploymentHostImagePullRecord> loc_imagePull;
    public ZonedDateTime                                lastActivity;

    //--//

    public static BackgroundActivityRecord scheduleTask(RecordLocked<DeploymentHostRecord> lock_targetHost,
                                                        RegistryTaggedImageRecord image,
                                                        boolean checkTemplate) throws
                                                                               Exception
    {
        Exceptions.requireNotNull(lock_targetHost, InvalidArgumentException.class, "No host provided");

        if (checkTemplate)
        {
            Exceptions.requireNotNull(image.findLabelOrDefault(WellKnownDockerImageLabel.ConfigTemplate, null), InvalidArgumentException.class, "No config template in image %s", image.getTag());
        }

        return BaseDeployTask.scheduleActivity(lock_targetHost, null, TaskForImagePull.class, (t) ->
        {
            SessionHolder        sessionHolder = lock_targetHost.getSessionHolder();
            DeploymentHostRecord targetHost    = lock_targetHost.get();

            t.loggerInstance.info("Provisioning host '%s' with image '%s'", targetHost.getHostId(), image.getTag());

            t.loc_image = sessionHolder.createLocator(image);

            t.imageTag       = image.getTag();
            t.imageTagTarget = targetHost.computeTargetTag(image);

            t.nextCheck = 0;

            if (targetHost.isArm32())
            {
                t.minimumCheckDelay = 30;
                t.extraTime         = Duration.of(12, ChronoUnit.HOURS);
            }
            else
            {
                t.minimumCheckDelay = 5;
                t.extraTime         = Duration.of(30, ChronoUnit.MINUTES);
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
        return String.format("Pull Docker Image '%s' on host '%s'", imageTag, getHostDisplayName());
    }

    @BackgroundActivityMethod(stateClass = State.class, initial = true, autoRetry = true)
    public CompletableFuture<Void> state_CheckImageLocally() throws
                                                             Exception
    {
        DeploymentHostImage cachedImage = withLocatorReadonly(getTargetHostLocator(), (sessionHolder, rec_host) ->
        {
            return rec_host.locateCachedImage(imageTag);
        });

        if (cachedImage != null && !cachedImage.isStale(6, TimeUnit.HOURS))
        {
            // We have the image in the list of images, skip pull.
            nextCheck = 0;
            return continueAtState(State.TaggingImage);
        }

        loggerInstance.debug("Parsing image '%s'...", imageTag);

        BuilderConfiguration cfg  = getServiceNonNull(BuilderConfiguration.class);
        UserInfo             user = null;

        if (cfg.credentials != null)
        {
            user = cfg.credentials.findFirstAutomationUser(WellKnownSites.dockerRegistry(), RoleType.Subscriber);
        }

        DockerImageDownloader dl = getServiceNonNull(DockerImageDownloader.class);
        try (DockerImageDownloader.Reservation reservation = await(dl.acquire()))
        {
            try
            {
                reservation.analyze(user, new DockerImageIdentifier(imageTag));
            }
            catch (NotFoundException e)
            {
                loggerInstance.error("Pull for image '%s' failed, due to %s", imageTag, e);
                nextCheck = Math.min(60, nextCheck + 5);
                return rescheduleDelayed(nextCheck, TimeUnit.SECONDS);
            }
        }

        return continueAtState(State.StartingPull);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_StartingPull() throws
                                                        Exception
    {
        loggerInstance.debug("Pulling '%s' on host '%s'", imageTag, getHostDisplayName());

        DeployLogicForAgent agentLogic = getLogicForAgent();
        pullToken     = await(agentLogic.startPullImage(imageTag));
        pullOffset    = 0;
        loc_imagePull = null;

        reportAgentReachable("Finally able to contact agent");

        lockedWithLocator(getTargetHostLocator(), 2, TimeUnit.MINUTES, (sessionHolder, lock_host) ->
        {
            DeploymentHostImagePullRecord rec_imagePull = DeploymentHostImagePullRecord.newInstance(lock_host.get(), sessionHolder.fromLocator(loc_image));
            sessionHolder.persistEntity(rec_imagePull);

            loc_imagePull = sessionHolder.createLocator(rec_imagePull);
        });

        nextCheck = 1;
        return continueAtState(State.CheckingPull);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_CheckingPull() throws
                                                        Exception
    {
        DeployLogicForAgent agentLogic = getLogicForAgent();
        DeployerDockerApi   proxy      = await(agentLogic.getProxy(DeployerDockerApi.class, 30));

        PullProgress pg;

        if (agentLogic.canSupport(DeploymentAgentFeature.ImagePullProgressEx2))
        {
            pg = await(proxy.checkPullImageProgressEx2(pullToken, pullOffset, 100));
        }
        else
        {
            pg = new PullProgress();

            int exitCode;

            if (agentLogic.canSupport(DeploymentAgentFeature.ImagePullProgressEx))
            {
                exitCode = await(proxy.checkPullImageProgressEx(pullToken, pullOffset, (line) ->
                {
                    pg.log.add(line);

                    return AsyncRuntime.NullResult;
                }));
            }
            else
            {
                exitCode = await(proxy.checkPullImageProgress(pullToken, (line) ->
                {
                    ShellOutput so = new ShellOutput();
                    so.timestamp = TimeUtils.now();
                    so.payload   = line;
                    pg.log.add(so);

                    return AsyncRuntime.NullResult;
                }));
            }

            switch (exitCode)
            {
                case 0: // Still executing.
                    pg.status = PullProgressStatus.Processing;
                    break;

                case 1: // Done.
                    pg.status = PullProgressStatus.Done;
                    break;

                case -1: // Failed.
                    pg.status = PullProgressStatus.Failed;
                    break;

                default: // Failure
                    pg.status = PullProgressStatus.UnknownToken;
                    break;
            }
        }

        // Extend timeout on activity.
        initializeTimeout(extraTime);

        if (loggerInstance.isEnabled(Severity.Debug))
        {
            for (ShellOutput entry : pg.log)
            {
                loggerInstance.debug("'%s': %s - %s", agentLogic.host_displayName, entry.timestamp, entry.payload);
            }
        }

        lockedWithLocatorOrNull(loc_imagePull, 30, TimeUnit.SECONDS, (sessionHolder, lock_imagePull) ->
        {
            if (lock_imagePull != null)
            {
                DeploymentHostImagePullRecord rec_imagePull = lock_imagePull.get();

                List<LogEntry> lst = Lists.newArrayList();

                for (ShellOutput shellOutput : pg.log)
                {
                    for (String payload : StringUtils.split(shellOutput.payload, "\n"))
                    {
                        LogEntry en = new LogEntry();
                        en.fd        = 1;
                        en.timestamp = shellOutput.timestamp;
                        en.line      = payload;
                        lst.add(en);
                    }
                }

                try (var logHandler = DeploymentHostImagePullRecord.allocateLogHandler(lock_imagePull))
                {
                    logHandler.removeDuplicates(lst);

                    switch (pg.status)
                    {
                        case Processing:
                            rec_imagePull.setUpdatedOn(TimeUtils.now());
                            break;

                        case Done:
                            if (!lst.isEmpty())
                            {
                                // As long as we get new output, pretend we are still processing.
                                pg.status = PullProgressStatus.Processing;
                                nextCheck = 0;
                            }
                            else
                            {
                                rec_imagePull.setStatus(JobStatus.COMPLETED);
                            }
                            break;

                        default: // Failure
                            if (rec_imagePull.getStatus() == JobStatus.EXECUTING)
                            {
                                rec_imagePull.setStatus(pg.status == PullProgressStatus.UnknownToken ? JobStatus.UNKNOWNTOKEN : JobStatus.FAILED);

                                LogEntry en = new LogEntry();
                                en.timestamp = TimeUtils.now();
                                en.line      = String.format("###### FAILED (token %s)", pullToken.id);
                                lst.add(en);
                            }
                            break;
                    }

                    try (LogHolder log = logHandler.newLogHolder())
                    {
                        for (LogEntry en : lst)
                        {
                            log.addLineSync(en.fd, en.timestamp, null, null, null, null, en.line);
                        }
                    }
                }
            }
        });

        if (!pg.log.isEmpty())
        {
            nextCheck = 0;
            pullOffset += pg.log.size();
        }
        pullRounds++;

        switch (pg.status)
        {
            case Processing:
                nextCheck = Math.min(180, nextCheck + minimumCheckDelay);
                return rescheduleDelayed(nextCheck, TimeUnit.SECONDS);

            case Done:
                nextCheck = 0;
                return continueAtState(State.TaggingImage);

            default: // Failure
                retries++;

                loggerInstance.error("Pull for image '%s' failed on host '%s' (attempt #%d)", imageTag, getHostDisplayName(), retries);

                // Don't wait.
                releaseTokenInBackground(agentLogic);

                switch (retries)
                {
                    case 3:
                    case 6:
                    case 10:
                    case 20:
                        sendAlertEmail();
                        break;

                    default:
                        if (retries % 30 == 0)
                        {
                            sendAlertEmail();
                        }
                        break;
                }

                nextCheck = 30;
                return continueAtState(State.CheckImageLocally, 30 * retries, TimeUnit.SECONDS);
        }
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_TaggingImage() throws
                                                        Exception
    {
        if (imageTagTarget != null)
        {
            DeployLogicForAgent agentLogic = getLogicForAgent();
            if (!await(agentLogic.tagImage(imageTag, imageTagTarget)))
            {
                reportAgentNotReachable("Failed to tag image '%s'", imageTagTarget);

                return continueAtState(State.CheckImageLocally, 30, TimeUnit.SECONDS);
            }
            else
            {
                reportAgentReachable("Finally able to tag image '%s'", imageTagTarget);

                // If this has a legacy version (like armhf), we want to tag it as well.
                String aliasTag = DeploymentHostImage.mapToLegacyVersion(imageTagTarget);
                if (aliasTag != null && !StringUtils.equals(aliasTag, imageTagTarget))
                {
                    imageTagTarget = aliasTag;
                    return rescheduleDelayed(0, null);
                }
            }
        }

        return markAsCompleted();
    }

    @Override
    public void cleanupOnFailure(Throwable t) throws
                                              Exception
    {
        lockedWithLocatorOrNull(loc_imagePull, 30, TimeUnit.SECONDS, (sessionHolder, lock_imagePull) ->
        {
            if (lock_imagePull != null)
            {
                DeploymentHostImagePullRecord rec_imagePull = lock_imagePull.get();
                if (rec_imagePull != null && rec_imagePull.getStatus() == JobStatus.EXECUTING)
                {
                    if (t instanceof TimeoutException)
                    {
                        rec_imagePull.setStatus(JobStatus.TIMEOUT);
                    }
                    else
                    {
                        try (var logHandler = DeploymentHostImagePullRecord.allocateLogHandler(lock_imagePull))
                        {
                            try (LogHolder log = logHandler.newLogHolder())
                            {
                                log.addLineSync(1, null, null, null, null, Severity.Error, String.format("###### FAILED with exception %s", t));
                            }
                        }

                        rec_imagePull.setStatus(JobStatus.FAILED);
                    }
                }
            }
        });

        // Don't wait.
        releaseTokenInBackground(null);
    }

    @Override
    public void cleanupOnComplete() throws
                                    Exception
    {
        lockedWithLocatorOrNull(loc_imagePull, 30, TimeUnit.SECONDS, (sessionHolder, lock_imagePull) ->
        {
            if (lock_imagePull != null)
            {
                DeploymentHostImagePullRecord rec_imagePull = lock_imagePull.get();

                if (rec_imagePull.getLastOffset() == 0)
                {
                    sessionHolder.deleteEntity(rec_imagePull);
                }
                else
                {
                    if (rec_imagePull.getStatus() == JobStatus.EXECUTING)
                    {
                        rec_imagePull.setStatus(JobStatus.COMPLETED);
                    }
                }
            }
        });

        // Don't wait.
        releaseTokenInBackground(null);
    }

    //--//

    private CompletableFuture<Void> releaseTokenInBackground(DeployLogicForAgent agentLogic) throws
                                                                                             Exception
    {
        ImagePullToken oldPullToken = pullToken;
        if (oldPullToken != null)
        {
            this.pullToken = null;

            if (agentLogic == null)
            {
                agentLogic = getLogicForAgent();
            }

            DeployerDockerApi proxy = await(agentLogic.getProxy(DeployerDockerApi.class, 30));

            try
            {
                for (int retry = 0; retry < 10; retry++)
                {
                    try
                    {
                        await(proxy.closePullImage(oldPullToken));
                        break;
                    }
                    catch (Throwable t)
                    {
                        // In case of connection error, sleep and retry.
                        await(sleep(15, TimeUnit.SECONDS));
                    }
                }
            }
            catch (Throwable t)
            {
                loggerInstance.error("Failed to release pull token: %s", t);
            }
        }

        return AsyncRuntime.NullResult;
    }

    private void sendAlertEmail() throws
                                  Exception
    {
        withLocatorReadonly(getTargetHostLocator(), (sessionHolder, rec_host) ->
        {
            ConfigVariables<ConfigVariable> parameters = s_template_stuck.allocate();

            parameters.setValue(ConfigVariable.HostSysId, getTargetHostLocator().getIdRaw());
            parameters.setValue(ConfigVariable.HostId, getHostDisplayName());
            parameters.setValue(ConfigVariable.Image, imageTag);
            parameters.setValue(ConfigVariable.Attempts, Integer.toString(retries));

            app.sendEmailNotification(BuilderApplication.EmailFlavor.Provisioning, rec_host.prepareEmailSubject("Image Pull Stuck In Loop"), parameters);
        });
    }
}
