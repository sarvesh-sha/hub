/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.build;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.NotFoundException;

import com.optio3.cloud.builder.logic.build.BuildLogicForDockerContainer;
import com.optio3.cloud.builder.model.jobs.JobStatus;
import com.optio3.cloud.builder.orchestration.state.DualPath;
import com.optio3.cloud.builder.orchestration.state.MavenState;
import com.optio3.cloud.builder.persistence.jobs.JobDefinitionStepRecordForDockerRun;
import com.optio3.cloud.builder.persistence.jobs.JobRecord;
import com.optio3.cloud.builder.persistence.jobs.JobStepRecord;
import com.optio3.cloud.builder.persistence.worker.HostRecord;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.concurrency.Executors;
import com.optio3.infra.WellKnownEnvironmentVariable;
import com.optio3.infra.WellKnownSites;
import com.optio3.util.Exceptions;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;

public class TaskForDockerRun extends BaseBuildTask
{
    public static final int MAX_RETRIES = 5;

    public boolean pulledImage;
    public int     pullRetries;

    public boolean startedContainer;

    //--//

    @Override
    public String getTitle()
    {
        return "Run command in Docker";
    }

    @Override
    public RecordLocator<? extends RecordWithCommonFields> getContext()
    {
        return null;
    }

    @BackgroundActivityMethod(needsSession = true)
    public void process(SessionHolder sessionHolder) throws
                                                     Exception
    {
        if (!pulledImage)
        {
            Exception e = pullImage(sessionHolder);

            dumpLogs(sessionHolder);

            if (e != null)
            {
                if (++pullRetries < MAX_RETRIES)
                {
                    rescheduleDelayed(1, TimeUnit.SECONDS);
                    return;
                }

                markAsFailed(e);
                return;
            }

            pulledImage = true;

            // Reschedule to persist the state.
            rescheduleDelayed(0, null);
            return;
        }

        if (!startedContainer)
        {
            startContainer(sessionHolder);

            startedContainer = true;

            // Reschedule to persist the state.
            rescheduleDelayed(0, null);
            return;
        }

        monitorExecution(sessionHolder);
    }

    private Exception pullImage(SessionHolder sessionHolder) throws
                                                             Exception
    {
        JobDefinitionStepRecordForDockerRun stepDef        = getCurrentStepDef(sessionHolder, JobDefinitionStepRecordForDockerRun.class);
        BuildLogicForDockerContainer        containerLogic = prepareLogic(sessionHolder, stepDef, false);
        String                              image          = containerLogic.getImage();

        if (stepDef.shouldForcePull())
        {
            try
            {
                containerLogic.removeImage(image, false);
            }
            catch (NotFoundException e)
            {
            }
        }

        try
        {
            RecordLocked<JobStepRecord> lock_step = newStep(sessionHolder, "Fetch Container Image '%s'", image);
            containerLogic.pullImage(lock_step, image);
            return null;
        }
        catch (NotFoundException e)
        {
            return e;
        }
    }

    private void startContainer(SessionHolder sessionHolder) throws
                                                             Exception
    {
        JobDefinitionStepRecordForDockerRun stepDef        = getCurrentStepDef(sessionHolder, JobDefinitionStepRecordForDockerRun.class);
        BuildLogicForDockerContainer        containerLogic = prepareLogic(sessionHolder, stepDef, true);

        RecordLocked<JobStepRecord> lock_step = newStep(sessionHolder, "Executing '%s'", stepDef.getCommandLine());
        JobStepRecord               rec_step  = lock_step.get();

        containerLogic.createContainer(rec_step);

        containerLogic.start(rec_step);
    }

    private void monitorExecution(SessionHolder sessionHolder) throws
                                                               Exception
    {
        RecordLocked<JobStepRecord> lock_step = getCurrentStep(sessionHolder);
        JobStepRecord               rec_step  = lock_step.get();
        JobRecord                   rec_job   = rec_step.getOwningJob();

        JobDefinitionStepRecordForDockerRun stepDef        = getCurrentStepDef(sessionHolder, JobDefinitionStepRecordForDockerRun.class);
        BuildLogicForDockerContainer        containerLogic = prepareLogic(sessionHolder, stepDef, false);

        if (rec_job.getStatus() == JobStatus.CANCELLING)
        {
            containerLogic.stop(lock_step);

            setStatusOfCurrentStep(sessionHolder, JobStatus.CANCELLED);
            markAsFailed("Command '%s' cancelled", stepDef.getCommandLine());
            return;
        }

        pollForLogs(lock_step, containerLogic, 10);

        if (rec_step.getStatus() == JobStatus.EXECUTING)
        {
            rescheduleDelayed(500, TimeUnit.MILLISECONDS);
            return;
        }

        Integer exitCodeBoxed = rec_step.getContainer()
                                        .getExitCode();
        if (exitCodeBoxed == null)
        {
            rescheduleDelayed(500, TimeUnit.MILLISECONDS);
            return;
        }

        // Extract the rest of the log.
        pollForLogs(lock_step, containerLogic, 20);

        int exitCode = exitCodeBoxed;

        LoggerInstance.info("ExitCode: %d %s", exitCode, rec_step.getStatus());

        if (exitCode != 0)
        {
            setStatusOfCurrentStep(sessionHolder, JobStatus.FAILED);
            markAsFailed("Command '%s' failed with exit code %d", stepDef.getCommandLine(), exitCode);
            return;
        }

        var cdnSettings = stepDef.getCdnSettings();
        if (!cdnSettings.isEmpty())
        {
            rec_job.callWithCdnHelper(sessionHolder, (cdnHelper, containerName) ->
            {
                Path workingDir = containerLogic.getWorkingDir();

                for (JobDefinitionStepRecordForDockerRun.CdnContent cdnContent : cdnSettings)
                {
                    Path guest = workingDir.resolve(cdnContent.relativeSourcePath);
                    Path host  = containerLogic.mapFromGuestToHost(guest);

                    Map<String, File> lst = cdnHelper.prepareListForUpload(host, cdnContent.publishPrefix);
                    cdnHelper.uploadContents(containerName, lst);
                }
            });
        }

        setStatusOfCurrentStep(sessionHolder, JobStatus.COMPLETED);
        markAsCompleted();
    }

    private void pollForLogs(RecordLocked<JobStepRecord> lock_step,
                             BuildLogicForDockerContainer containerLogic,
                             int timeout) throws
                                          Exception
    {
        MonotonousTime pollForLog = TimeUtils.computeTimeoutExpiration(timeout, TimeUnit.SECONDS);
        while (!TimeUtils.isTimeoutExpired(pollForLog))
        {
            if (!containerLogic.refresh(lock_step))
            {
                break;
            }

            Executors.safeSleep(50);
        }

        dumpLogs(lock_step.getSessionHolder());
    }

    //--//

    private BuildLogicForDockerContainer prepareLogic(SessionHolder sessionHolder,
                                                      JobDefinitionStepRecordForDockerRun stepDef,
                                                      boolean initCDN)
    {
        HostRecord rec_host = getTargetHost(sessionHolder);
        JobRecord  rec_job  = getJob(sessionHolder);

        String image = WellKnownSites.makeDockerImageTagForPull(stepDef.getImage());

        BuildLogicForDockerContainer containerLogic = new BuildLogicForDockerContainer(appConfig, sessionHolder, rec_host, rec_job);

        containerLogic.setImage(image);
        containerLogic.setCommandLine(stepDef.getCommandLine());
        containerLogic.setWorkingDir(Paths.get(resolveVariablesToText(stepDef.getWorkingDirectory())));

        for (String binding : stepDef.getBindings())
        {
            SubstitutionContext sc = resolveVariables(binding);

            if (appConfig.developerSettings.useLocalMaven)
            {
                boolean found = false;

                for (MavenState stateMaven : sc.getReferences(MavenState.class))
                {
                    Path home = Paths.get(System.getenv("HOME"), ".m2");
                    containerLogic.addBind(home, stateMaven.dir.guestPath);
                    found = true;
                    break;
                }

                if (found)
                {
                    continue;
                }
            }

            DualPath v = sc.findSource(DualPath.class, 0);
            if (v == null)
            {
                throw Exceptions.newIllegalArgumentException("No reference to virtual directory for '%s'", sc.result);
            }

            containerLogic.addBind(sessionHolder.fromLocator(v.hostDir), v.guestPath);
        }

        Map<String, String> envVars = stepDef.getEnvironmentVariables();
        for (String key : envVars.keySet())
        {
            containerLogic.addEnvironmentVariable(key, envVars.get(key));
        }

        containerLogic.addEnvironmentVariable(WellKnownEnvironmentVariable.BuildBranch, getCommonState().branch);
        containerLogic.addEnvironmentVariable(WellKnownEnvironmentVariable.BuildCommit, getCommonState().commit);
        containerLogic.addEnvironmentVariable(WellKnownEnvironmentVariable.BuildProd, true);

        if (initCDN)
        {
            var cdnContent = stepDef.getCdnSettings();
            if (!cdnContent.isEmpty())
            {
                rec_job.callWithCdnHelper(sessionHolder, (cdnHelper, containerName) ->
                {
                    containerLogic.addEnvironmentVariable(WellKnownEnvironmentVariable.BuildDeployUrl, cdnHelper.getURL(containerName));
                });
            }
        }

        containerLogic.allowAccessToDockerDaemon();

        return containerLogic;
    }
}
