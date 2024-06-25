/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.build;

import java.util.concurrent.TimeUnit;

import com.optio3.cloud.builder.model.jobs.JobStatus;
import com.optio3.cloud.builder.persistence.jobs.JobDefinitionStepRecordForSshCommand;
import com.optio3.cloud.builder.persistence.jobs.JobStepRecord;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.persistence.LogHolder;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.infra.SshHelper;
import com.optio3.infra.directory.SshKey;
import com.optio3.util.Exceptions;

public class TaskForSshCommand extends BaseBuildTask
{
    @Override
    public String getTitle()
    {
        return "SSH command";
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
        JobDefinitionStepRecordForSshCommand stepDef = getCurrentStepDef(sessionHolder, JobDefinitionStepRecordForSshCommand.class);

        String commandLine = stepDef.getCommandLine();
        String targetHost  = stepDef.getTargetHost();
        String cred        = stepDef.getCredentials();

        RecordLocked<JobStepRecord> lock_step = newStep(sessionHolder, "Executing '%s' on '%s'", commandLine, targetHost);

        String[] credParts = cred.split("@");
        if (credParts.length != 2)
        {
            throw Exceptions.newIllegalArgumentException("Invalid format for credentials (it should be <user>@<infra>): %s", cred);
        }

        String user = credParts[0];
        String site = credParts[1];
        SshKey key  = appConfig.credentials.findFirstSshKey(site, user);

        int exitCode;

        try (SshHelper helper = new SshHelper(key, targetHost, user))
        {
            try (var logHandler = JobStepRecord.allocateLogHandler(lock_step))
            {
                try (LogHolder log = logHandler.newLogHolder())
                {
                    exitCode = helper.exec(commandLine,
                                           1,
                                           TimeUnit.MINUTES,
                                           (stdOut) -> log.addLineSync(1, null, null, null, null, null, stdOut),
                                           (stdErr) -> log.addLineSync(2, null, null, null, null, null, stdErr));
                }
            }
        }

        if (exitCode != 0)
        {
            setStatusOfCurrentStep(sessionHolder, JobStatus.FAILED);
            markAsFailed("Command '%s' failed with exit code %d", stepDef.getCommandLine(), exitCode);
            return;
        }

        setStatusOfCurrentStep(sessionHolder, JobStatus.COMPLETED);
        markAsCompleted();
    }
}
