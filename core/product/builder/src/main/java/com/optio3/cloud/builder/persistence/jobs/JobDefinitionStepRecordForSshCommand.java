/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.jobs;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.builder.model.jobs.JobDefinitionStepForSshCommand;
import com.optio3.cloud.builder.orchestration.tasks.build.BaseBuildTask;
import com.optio3.cloud.builder.orchestration.tasks.build.TaskForSshCommand;

@Entity
@Table(name = "JOB_DEF_STEP__SSH_COMMAND")
@Optio3TableInfo(externalId = "JobDefinitionStepForSshCommand", model = JobDefinitionStepForSshCommand.class, metamodel = JobDefinitionStepRecordForSshCommand_.class)
public class JobDefinitionStepRecordForSshCommand extends JobDefinitionStepRecord
{
    @Column(name = "credentials", nullable = false)
    private String credentials;

    @Column(name = "target_host", nullable = false)
    private String targetHost;

    @Column(name = "command_line", nullable = false)
    private String commandLine;

    //--//

    public JobDefinitionStepRecordForSshCommand()
    {
    }

    public JobDefinitionStepRecordForSshCommand(JobDefinitionRecord job)
    {
        super(job);
    }

    //--//

    public String getCredentials()
    {
        return credentials;
    }

    public void setCredentials(String credentials)
    {
        this.credentials = credentials;
    }

    public String getTargetHost()
    {
        return targetHost;
    }

    public void setTargetHost(String targetHost)
    {
        this.targetHost = targetHost;
    }

    public String getCommandLine()
    {
        return commandLine;
    }

    public void setCommandLine(String commandLine)
    {
        this.commandLine = commandLine;
    }

    //--//

    @Override
    public boolean requiresCdn()
    {
        return false;
    }

    @Override
    public Class<? extends BaseBuildTask> getHandler()
    {
        return TaskForSshCommand.class;
    }
}
