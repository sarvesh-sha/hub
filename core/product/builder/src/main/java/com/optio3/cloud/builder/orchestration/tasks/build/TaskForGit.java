/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.build;

import com.optio3.cloud.builder.logic.build.BuildLogicForRepository;
import com.optio3.cloud.builder.model.jobs.input.CommitDetails;
import com.optio3.cloud.builder.orchestration.state.DualPath;
import com.optio3.cloud.builder.orchestration.state.RepositoryState;
import com.optio3.cloud.builder.persistence.jobs.JobDefinitionStepRecordForGit;
import com.optio3.cloud.builder.persistence.jobs.JobRecord;
import com.optio3.cloud.builder.persistence.jobs.JobSourceRecord;
import com.optio3.cloud.builder.persistence.jobs.JobStepRecord;
import com.optio3.cloud.builder.persistence.jobs.input.RepositoryRecord;
import com.optio3.cloud.builder.persistence.worker.HostRecord;
import com.optio3.cloud.builder.persistence.worker.RepositoryCheckoutRecord;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;

public class TaskForGit extends BaseBuildTask
{
    @Override
    public String getTitle()
    {
        return "Git checkout";
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
        JobRecord  rec_job  = getJob(sessionHolder);
        HostRecord rec_host = getTargetHost(sessionHolder);

        JobDefinitionStepRecordForGit rec_stepDef = getCurrentStepDef(sessionHolder, JobDefinitionStepRecordForGit.class);
        RepositoryRecord              rec_repo    = rec_stepDef.getRepo();

        //
        // 1) Create Step
        //
        RecordLocked<JobStepRecord> lock_step = newStep(sessionHolder, "Cloning '%s'", rec_repo.getGitUrl());

        sessionHolder.commitAndBeginNewTransaction();

        //
        // 2) Clone repository
        //
        LoggerInstance.info("Cloning Repository '%s'...", rec_repo.getGitUrl());
        BuildLogicForRepository  repoLogic     = new BuildLogicForRepository(appConfig, sessionHolder, rec_host, rec_job);
        RepositoryCheckoutRecord repo_checkout = repoLogic.acquire(lock_step, rec_repo);
        LoggerInstance.info("Repository cloned.");

        RepositoryState state = new RepositoryState();
        state.dir = DualPath.newInstance(sessionHolder, repo_checkout.getDirectoryForWork(), rec_stepDef.getDirectory());
        putStateValue(rec_stepDef.getBuildId(), state);

        sessionHolder.commitAndBeginNewTransaction();
        dumpLogs(sessionHolder);

        LoggerInstance.info("Switching to branch '%s'...", getCommonState().branch);
        CommitDetails details = repoLogic.switchToBranch(lock_step, repo_checkout, getCommonState().branch, getCommonState().commit);

        JobSourceRecord rec_jobSource = JobSourceRecord.newInstance(rec_job, rec_repo, details.branch, details.id);
        sessionHolder.persistEntity(rec_jobSource);

        repo_checkout.setCurrentBranch(details.branch);
        repo_checkout.setCurrentCommit(details.id);
        LoggerInstance.info("Switched branch: %s - %s - %s - %s", details.branch, details.author.emailAddress, details.author.when, details.message);

        markAsCompleted();
    }
}
