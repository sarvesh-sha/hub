/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.logic;

import com.optio3.cloud.builder.BuilderApplication;
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.logic.build.BuildLogicForRepository;
import com.optio3.cloud.builder.model.jobs.JobStatus;
import com.optio3.cloud.builder.model.jobs.input.RepositoryRefresh;
import com.optio3.cloud.builder.persistence.jobs.JobRecord;
import com.optio3.cloud.builder.persistence.jobs.JobStepRecord;
import com.optio3.cloud.builder.persistence.jobs.input.RepositoryRecord;
import com.optio3.cloud.builder.persistence.worker.HostRecord;
import com.optio3.cloud.builder.persistence.worker.RepositoryCheckoutRecord;
import com.optio3.cloud.db.Optio3DbRateLimiter;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.logging.Logger;
import com.optio3.service.IServiceProvider;

public class RepositoryLogic
{
    public static final Logger LoggerInstance = new Logger(RepositoryLogic.class);

    protected final IServiceProvider m_serviceProvider;

    public RepositoryLogic(IServiceProvider serviceProvider)
    {
        m_serviceProvider = serviceProvider;
    }

    public RepositoryRefresh refreshAll()
    {
        RepositoryRefresh status = new RepositoryRefresh();

        try (SessionHolder holder = SessionHolder.createWithNewSessionWithTransaction(m_serviceProvider, null, Optio3DbRateLimiter.Normal))
        {
            RecordHelper<RepositoryRecord> helper = holder.createHelper(RepositoryRecord.class);

            for (RecordIdentity ri : RepositoryRecord.list(helper))
            {
                RepositoryRecord rec_repo = helper.get(ri.sysId);

                try
                {
                    refreshBranchesAndCommits(holder, rec_repo, status);
                }
                catch (Exception e)
                {
                    LoggerInstance.error("Failed to refresh branches and commits for repo '%s': %s", rec_repo.getGitUrl(), e);
                }
            }

            holder.commit();
        }

        return status;
    }

    public void refreshBranchesAndCommits(SessionHolder sessionHolder,
                                          RepositoryRecord rec_repo,
                                          RepositoryRefresh status) throws
                                                                    Exception
    {
        BuilderApplication   app = m_serviceProvider.getServiceNonNull(BuilderApplication.class);
        BuilderConfiguration cfg = m_serviceProvider.getServiceNonNull(BuilderConfiguration.class);

        RecordHelper<JobRecord>     helperJob     = sessionHolder.createHelper(JobRecord.class);
        RecordHelper<JobStepRecord> helperJobStep = sessionHolder.createHelper(JobStepRecord.class);

        HostRecord rec_host = app.getCurrentHost(sessionHolder);

        LoggerInstance.info("Refreshing branches and commits for %s...", rec_repo.getGitUrl());

        //
        // To update commit records, we need to have access to a checkout of the repo.
        // But to use a checkout, it has to be associated with a job.
        // So we create a temporary job.
        //
        JobRecord rec_job = new JobRecord();
        rec_job.setStatus(JobStatus.CREATED);
        rec_job.setName("<Internal Job for Repository update>");
        rec_job.setIdPrefix("<Internal Job for Repository update>");
        helperJob.persist(rec_job);

        JobStepRecord rec_step = JobStepRecord.newInstance(rec_job);
        rec_step.setName("<Internal Job for Repository update>");
        RecordLocked<JobStepRecord> lock_step = helperJobStep.persist(rec_step);

        BuildLogicForRepository  repoLogic        = new BuildLogicForRepository(cfg, sessionHolder, rec_host, rec_job);
        RepositoryCheckoutRecord rec_repoCheckout = repoLogic.acquire(lock_step, rec_repo);

        sessionHolder.commitAndBeginNewTransaction();

        repoLogic.synchronizeRepositoryRecords(rec_repoCheckout, status);

        repoLogic.release(rec_repoCheckout);

        helperJob.delete(rec_job);

        sessionHolder.commitAndBeginNewTransaction();

        LoggerInstance.info("Done refreshing branches and commits for %s", rec_repo.getGitUrl());
    }
}
