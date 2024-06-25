/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.jobs;

import java.util.Map;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinTable;
import javax.persistence.Table;

import com.google.common.collect.Maps;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.builder.model.jobs.JobDefinitionStepForDockerBuild;
import com.optio3.cloud.builder.orchestration.tasks.build.BaseBuildTask;
import com.optio3.cloud.builder.orchestration.tasks.build.TaskForDockerBuild;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;

@Entity
@Table(name = "JOB_DEF_STEP__DOCKER_BUILD")
@Optio3TableInfo(externalId = "JobDefinitionStepForDockerBuild", model = JobDefinitionStepForDockerBuild.class, metamodel = JobDefinitionStepRecordForDockerBuild_.class)
public class JobDefinitionStepRecordForDockerBuild extends JobDefinitionStepRecord
{
    @Column(name = "source_path", nullable = false)
    private String sourcePath;

    @Column(name = "docker_file", nullable = false)
    private String dockerFile;

    @Enumerated(EnumType.STRING)
    @Column(name = "architecture", nullable = false)
    private DockerImageArchitecture architecture;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_service", nullable = false)
    private DeploymentRole targetService;

    @ElementCollection
    @JoinTable(name = "DOCKER_BUILD__BUILDARGS")
    private Map<String, String> buildArgs = Maps.newHashMap();

    //--//

    public JobDefinitionStepRecordForDockerBuild()
    {
    }

    public JobDefinitionStepRecordForDockerBuild(JobDefinitionRecord job)
    {
        super(job);
    }

    //--//

    public String getSourcePath()
    {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath)
    {
        this.sourcePath = sourcePath;
    }

    public String getDockerFile()
    {
        return dockerFile;
    }

    public void setDockerFile(String dockerFile)
    {
        this.dockerFile = dockerFile;
    }

    public DockerImageArchitecture getArchitecture()
    {
        return architecture;
    }

    public void setArchitecture(DockerImageArchitecture architecture)
    {
        this.architecture = architecture;
    }

    public DeploymentRole getTargetService()
    {
        return targetService;
    }

    public void setTargetService(DeploymentRole targetService)
    {
        this.targetService = targetService;
    }

    public Map<String, String> getBuildArgs()
    {
        return buildArgs;
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
        return TaskForDockerBuild.class;
    }
}
