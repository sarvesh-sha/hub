/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.build;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.optio3.cloud.builder.HostRemoter;
import com.optio3.cloud.builder.model.jobs.JobStatus;
import com.optio3.cloud.builder.orchestration.AbstractBuilderActivityHandler;
import com.optio3.cloud.builder.persistence.jobs.JobDefinitionRecord;
import com.optio3.cloud.builder.persistence.jobs.JobDefinitionStepRecord;
import com.optio3.cloud.builder.persistence.jobs.JobRecord;
import com.optio3.cloud.builder.persistence.jobs.JobStepRecord;
import com.optio3.cloud.builder.persistence.worker.HostRecord;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;

public class CommonBuildState extends AbstractBuilderActivityHandler.GenericValue
{
    public RecordLocator<JobRecord>           loc_job;
    public RecordLocator<JobDefinitionRecord> loc_jobDef;
    public RecordLocator<HostRecord>          loc_targetHost;

    public String branch;
    public String commit;

    //--//

    public int currentJobStep;

    //--//

    public RecordLocator<JobStepRecord> locatorForCurrentStep;
    public int                          nextLineForCurrentStep;

    //--//

    public CommonBuildState()
    {
        // Jackson deserialization
    }

    public CommonBuildState(SessionHolder sessionHolder,
                            JobRecord job,
                            HostRecord targetHost,
                            JobDefinitionRecord jobDef)
    {
        this.loc_job        = sessionHolder.createLocator(job);
        this.loc_targetHost = sessionHolder.createLocator(targetHost);
        this.loc_jobDef     = sessionHolder.createLocator(jobDef);
    }

    //--//

    public void cleanup(HostRemoter hostRemoter,
                        SessionHolder sessionHolder) throws
                                                     Exception
    {
        JobRecord rec_job = sessionHolder.fromLocator(loc_job);

        try (ValidationResultsHolder validation = new ValidationResultsHolder(sessionHolder, null, false))
        {
            for (JobStepRecord rec_step : Lists.newArrayList(rec_job.getSteps()))
            {
                rec_step.freeResources(hostRemoter, validation);
            }
        }

        rec_job.releaseResources(hostRemoter, sessionHolder, false);

        if (rec_job.conditionallyChangeStatus(JobStatus.EXECUTING, JobStatus.FAILED))
        {
            rec_job.sendEmail(sessionHolder);
        }
    }

    //--//

    public HostRecord getTargetHost(SessionHolder sessionHolder)
    {
        return sessionHolder.fromLocator(loc_targetHost);
    }

    //--//

    public JobRecord getJob(SessionHolder sessionHolder)
    {
        return sessionHolder.fromLocatorOrNull(loc_job);
    }

    public RecordLocked<JobStepRecord> getCurrentStep(SessionHolder sessionHolder)
    {
        return sessionHolder.fromLocatorWithLockOrNull(locatorForCurrentStep, 30, TimeUnit.SECONDS);
    }

    //--//

    public JobDefinitionRecord getJobDef(SessionHolder sessionHolder)
    {
        return sessionHolder.fromLocator(loc_jobDef);
    }

    public <T extends JobDefinitionStepRecord> T getCurrentStepDef(SessionHolder sessionHolder,
                                                                   Class<T> clz)
    {
        List<JobDefinitionStepRecord> steps = getJobDef(sessionHolder).getSteps();
        if (currentJobStep < steps.size())
        {
            return clz.cast(steps.get(currentJobStep));
        }

        return null;
    }
}
