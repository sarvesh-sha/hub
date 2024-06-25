/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.build;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.cloud.builder.model.jobs.JobStatus;
import com.optio3.cloud.builder.orchestration.AbstractBuilderActivityHandler;
import com.optio3.cloud.builder.persistence.config.UserRecord;
import com.optio3.cloud.builder.persistence.jobs.JobDefinitionRecord;
import com.optio3.cloud.builder.persistence.jobs.JobDefinitionStepRecord;
import com.optio3.cloud.builder.persistence.jobs.JobRecord;
import com.optio3.cloud.builder.persistence.jobs.JobStepRecord;
import com.optio3.cloud.builder.persistence.worker.HostRecord;
import com.optio3.cloud.logic.BackgroundActivityHandler;
import com.optio3.cloud.persistence.LogEntry;
import com.optio3.cloud.persistence.LogHolder;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.infra.WellKnownSites;
import com.optio3.logging.Logger;
import com.optio3.text.AnsiParser;
import com.optio3.util.Exceptions;

public abstract class BaseBuildTask extends AbstractBuilderActivityHandler implements BackgroundActivityHandler.ICleanupOnCompleteWithSession,
                                                                                      BackgroundActivityHandler.ICleanupOnFailureWithSession,
                                                                                      BackgroundActivityHandler.IPostProcessWithSession
{
    public static final Logger LoggerInstance = new Logger(BaseBuildTask.class);

    //--//

    public static JobRecord schedule(RecordLocked<JobDefinitionRecord> lock_jobDef,
                                     String jobName,
                                     HostRecord targetHost,
                                     String branch,
                                     String commit,
                                     UserRecord user) throws
                                                      Exception
    {
        JobDefinitionRecord jobDef = lock_jobDef.get();

        JobRecord rec_job = JobRecord.newInstance(jobDef, branch, commit, user);
        rec_job.setName(jobName);
        rec_job.setStatus(JobStatus.EXECUTING);

        String idPrefix = jobDef.getIdPrefix();
        if (branch != null)
        {
            idPrefix += "_" + branch.replace('/', '_');
        }

        LocalDateTime date = LocalDateTime.now();
        idPrefix += date.format(DateTimeFormatter.ofPattern("'_'uuuuMMdd'_'HHmm"));

        // Assign a unique Id Prefix to the rec_job.
        SessionHolder           sessionHolder = lock_jobDef.getSessionHolder();
        RecordHelper<JobRecord> helper        = sessionHolder.createHelper(JobRecord.class);
        int                     seq           = 0;
        while (true)
        {
            String idPrefixUnique = idPrefix;

            if (seq > 0)
            {
                idPrefixUnique += "_" + seq;
            }

            if (JobRecord.findByPrefix(helper, idPrefixUnique) == null)
            {
                rec_job.setIdPrefix(idPrefixUnique);
                break;
            }

            seq++;
        }

        sessionHolder.persistEntity(rec_job);

        //--//

        CommonBuildState js = new CommonBuildState(sessionHolder, rec_job, targetHost, jobDef);
        js.branch = branch;
        js.commit = commit;

        //--//

        JobDefinitionStepRecord rec_stepDef = js.getCurrentStepDef(sessionHolder, JobDefinitionStepRecord.class);

        Class<? extends BaseBuildTask> clz = rec_stepDef.getHandler();

        BaseBuildTask newHandler = BackgroundActivityHandler.allocate(clz);
        newHandler.putStateValue("<root>", js);
        newHandler.configureContext();

        newHandler.schedule(lock_jobDef.getSessionHolder(), null);

        return rec_job;
    }

    //--//

    @Override
    public void configureContext()
    {
        // Nothing to do.
    }

    @Override
    public void postProcess(SessionHolder sessionHolder,
                            Throwable t) throws
                                         Exception
    {
        if (t != null)
        {
            try
            {
                RecordLocked<JobStepRecord> lock_step = getCurrentStep(sessionHolder);
                if (lock_step != null)
                {
                    try (var logHandler = JobStepRecord.allocateLogHandler(lock_step))
                    {
                        try (LogHolder logHolder = logHandler.newLogHolder())
                        {
                            logHolder.addTextSync(Exceptions.convertStackTraceToString(t));
                        }
                    }
                }
            }
            catch (Throwable t2)
            {
                LoggerInstance.error("Failed to save exception details to step: %s", t);
                LoggerInstance.error("Reason: %s", t2);
            }
        }

        JobRecord rec_job = getJob(sessionHolder);

        rec_job.conditionallyChangeStatus(JobStatus.CANCELLING, JobStatus.CANCELLED);

        if (rec_job.getStatus() == JobStatus.CANCELLED)
        {
            getCommonState().cleanup(hostRemoter, sessionHolder);
        }

        dumpLogs(sessionHolder);
    }

    @Override
    public void cleanupOnFailure(SessionHolder sessionHolder,
                                 Throwable t) throws
                                              Exception
    {
        setStatusOfCurrentStep(sessionHolder, JobStatus.FAILED);

        JobRecord rec_job = getJob(sessionHolder);
        rec_job.conditionallyChangeStatus(JobStatus.CANCELLING, JobStatus.CANCELLED);
        if (rec_job.conditionallyChangeStatus(JobStatus.EXECUTING, JobStatus.FAILED))
        {
            rec_job.sendEmail(sessionHolder);
        }

        getCommonState().cleanup(hostRemoter, sessionHolder);
    }

    @Override
    public void cleanupOnComplete(SessionHolder sessionHolder) throws
                                                               Exception
    {
        setStatusOfCurrentStep(sessionHolder, JobStatus.COMPLETED);

        moveToNextStep(sessionHolder);
    }

    private void moveToNextStep(SessionHolder sessionHolder) throws
                                                             Exception
    {
        CommonBuildState state = getCommonState();

        JobRecord rec_job = getJob(sessionHolder);

        if (rec_job.conditionallyChangeStatus(JobStatus.CANCELLING, JobStatus.CANCELLED))
        {
            state.cleanup(hostRemoter, sessionHolder);
            return;
        }

        state.currentJobStep++;

        Class<? extends BaseBuildTask> clz;

        JobDefinitionStepRecord rec_stepDef = state.getCurrentStepDef(sessionHolder, JobDefinitionStepRecord.class);
        if (rec_stepDef == null)
        {
            if (rec_job.conditionallyChangeStatus(JobStatus.EXECUTING, JobStatus.COMPLETED))
            {
                rec_job.sendEmail(sessionHolder);
            }

            state.cleanup(hostRemoter, sessionHolder);
            return;
        }

        clz = rec_stepDef.getHandler();
        scheduleNextHandler(sessionHolder, clz);
    }

    //--//

    protected CommonBuildState getCommonState()
    {
        return getStateValue(CommonBuildState.class, "<root>");
    }

    protected JobRecord getJob(SessionHolder sessionHolder)
    {
        return getCommonState().getJob(sessionHolder);
    }

    protected <T extends JobDefinitionStepRecord> T getCurrentStepDef(SessionHolder sessionHolder,
                                                                      Class<T> clz)
    {
        return getCommonState().getCurrentStepDef(sessionHolder, clz);
    }

    protected HostRecord getTargetHost(SessionHolder sessionHolder)
    {
        return getCommonState().getTargetHost(sessionHolder);
    }

    protected RecordLocked<JobStepRecord> getCurrentStep(SessionHolder sessionHolder)
    {
        return getCommonState().getCurrentStep(sessionHolder);
    }

    protected void setStatusOfCurrentStep(SessionHolder sessionHolder,
                                          JobStatus status)
    {
        RecordLocked<JobStepRecord> lock_step = getCurrentStep(sessionHolder);
        if (lock_step != null)
        {
            JobStepRecord rec_step = lock_step.get();
            switch (rec_step.getStatus())
            {
                case COMPLETED:
                case FAILED:
                    break;

                default:
                    rec_step.setStatus(status);
                    break;
            }
        }
    }

    //--//

    protected RecordLocked<JobStepRecord> newStep(SessionHolder sessionHolder,
                                                  String fmt,
                                                  Object... args)
    {
        RecordLocked<JobStepRecord> lock_previousStep = getCurrentStep(sessionHolder);
        if (lock_previousStep != null)
        {
            JobStepRecord rec_previousStep = lock_previousStep.get();
            rec_previousStep.setStatus(JobStatus.COMPLETED);
        }

        String name = String.format(fmt, args);

        JobStepRecord rec_newStep = JobStepRecord.newInstance(getJob(sessionHolder));
        rec_newStep.setName(name);
        rec_newStep.setStatus(JobStatus.EXECUTING);
        RecordLocked<JobStepRecord> lock_newStep = sessionHolder.persistEntity(rec_newStep);

        CommonBuildState state = getCommonState();

        state.locatorForCurrentStep  = sessionHolder.createLocator(rec_newStep);
        state.nextLineForCurrentStep = 0;

        return lock_newStep;
    }

    protected List<LogEntry> getNewOutputFromStep(SessionHolder sessionHolder) throws
                                                                               Exception
    {
        CommonBuildState state = getCommonState();

        List<LogEntry> res = Lists.newArrayList();

        RecordLocked<JobStepRecord> lock_step = getCurrentStep(sessionHolder);
        if (lock_step != null)
        {
            try (var logHandler = JobStepRecord.allocateLogHandler(lock_step))
            {
                logHandler.extract(state.nextLineForCurrentStep, null, null, (item, offset) ->
                {
                    res.add(item);
                    state.nextLineForCurrentStep = offset + 1;
                });
            }
        }

        return res;
    }

    protected void dumpLogs(SessionHolder sessionHolder) throws
                                                         Exception
    {
        if (appConfig.developerSettings.dumpBuildLogs)
        {
            AnsiParser    ansiParser = new AnsiParser();
            StringBuilder sb         = new StringBuilder();

            for (LogEntry item : getNewOutputFromStep(sessionHolder))
            {
                List<Object> parsedLine = ansiParser.parse(item.line);
                sb.setLength(0);
                for (Object o : parsedLine)
                {
                    if (o instanceof String)
                    {
                        sb.append((String) o);
                    }
                }

                String text = sb.toString();
                if (text.endsWith("\n"))
                {
                    text = text.substring(0, text.length() - 1);
                }

                LoggerInstance.info("%s: %s - %s", item.fd, item.timestamp.toLocalDateTime(), text);
            }
        }
    }

    //--//

    protected String getNexusRepoForSnapshots()
    {
        return WellKnownSites.nexusRepository("maven-snapshots");
    }

    protected String getNexusRepoForReleases()
    {
        return WellKnownSites.nexusRepository("maven-releases");
    }

    protected String getNexusRepo(String repo)
    {
        return WellKnownSites.nexusRepository(repo);
    }

    //--//

    protected void scheduleNextHandler(SessionHolder sessionHolder,
                                       Class<? extends BaseBuildTask> clz) throws
                                                                           Exception
    {
        BaseBuildTask newHandler = allocate(clz);

        scheduleNextHandler(sessionHolder, newHandler);
    }
}
