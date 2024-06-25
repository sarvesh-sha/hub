/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.jobs;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.builder.model.jobs.JobDefinitionStepForDockerRun;
import com.optio3.cloud.builder.orchestration.tasks.build.BaseBuildTask;
import com.optio3.cloud.builder.orchestration.tasks.build.TaskForDockerRun;
import com.optio3.cloud.persistence.MetadataField;

@Entity
@Table(name = "JOB_DEF_STEP__DOCKER_RUN")
@Optio3TableInfo(externalId = "JobDefinitionStepForDockerRun", model = JobDefinitionStepForDockerRun.class, metamodel = JobDefinitionStepRecordForDockerRun_.class,
                 metadata = JobDefinitionStepRecordForDockerRun.WellKnownMetadata.class)
public class JobDefinitionStepRecordForDockerRun extends JobDefinitionStepRecord
{
    public static class WellKnownMetadata implements Optio3TableInfo.IMetadataDigest
    {
        public static final TypeReference<List<CdnContent>> s_typeRef_Cdn = new TypeReference<>()
        {
        };

        public static final MetadataField<Map<String, String>> environmentVariables = new MetadataField<>("environmentVariables", MetadataField.TypeRef_mapOfStrings, Maps::newHashMap);

        public static final MetadataField<Set<String>> bindings = new MetadataField<>("bindings", MetadataField.TypeRef_setOfStrings, Sets::newHashSet);

        public static final MetadataField<List<CdnContent>> cdnContent = new MetadataField<>("cdnContent", s_typeRef_Cdn, Lists::newArrayList);
    }

    public static class CdnContent
    {
        public String relativeSourcePath;
        public String publishPrefix;
    }

    //--//

    @Column(name = "image", nullable = false)
    private String image;

    @Column(name = "working_directory", nullable = false)
    private String workingDirectory;

    @Column(name = "command_line", nullable = false)
    private String commandLine;

    @Column(name = "force_pull")
    private boolean forcePull;

    //--//

    public JobDefinitionStepRecordForDockerRun()
    {
    }

    public JobDefinitionStepRecordForDockerRun(JobDefinitionRecord job)
    {
        super(job);
    }

    //--//

    public String getImage()
    {
        return image;
    }

    public void setImage(String image)
    {
        this.image = image;
    }

    public String getWorkingDirectory()
    {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory)
    {
        this.workingDirectory = workingDirectory;
    }

    public String getCommandLine()
    {
        return commandLine;
    }

    public void setCommandLine(String commandLine)
    {
        this.commandLine = commandLine;
    }

    public boolean shouldForcePull()
    {
        return forcePull;
    }

    public void setForcePull(boolean forcePull)
    {
        this.forcePull = forcePull;
    }

    //--//

    public Map<String, String> getEnvironmentVariables()
    {
        return getMetadata(WellKnownMetadata.environmentVariables);
    }

    public void setEnvironmentVariables(Map<String, String> environmentVariables)
    {
        putMetadata(WellKnownMetadata.environmentVariables, environmentVariables);
    }

    public void setEnvironmentVariable(String key,
                                       String value)
    {
        Map<String, String> map = getEnvironmentVariables();

        map.put(key, value);

        setEnvironmentVariables(map);
    }

    //--//

    public Set<String> getBindings()
    {
        return getMetadata(WellKnownMetadata.bindings);
    }

    public void setBindings(Set<String> bindings)
    {
        putMetadata(WellKnownMetadata.bindings, bindings);
    }

    public void addBinding(String binding)
    {
        Set<String> bindings = getBindings();

        bindings.add(binding);

        setBindings(bindings);
    }

    //--//

    public List<CdnContent> getCdnSettings()
    {
        return getMetadata(WellKnownMetadata.cdnContent);
    }

    public void setCdnSettings(List<CdnContent> settings)
    {
        putMetadata(WellKnownMetadata.cdnContent, settings);
    }

    public void publishToCdn(String relativeSourcePath,
                             String publishPrefix)
    {
        List<CdnContent> settings = getCdnSettings();

        CdnContent obj = new CdnContent();
        obj.relativeSourcePath = relativeSourcePath;
        obj.publishPrefix      = publishPrefix;

        settings.add(obj);

        setCdnSettings(settings);
    }

    //--//

    @Override
    public boolean requiresCdn()
    {
        return !getCdnSettings().isEmpty();
    }

    @Override
    public Class<? extends BaseBuildTask> getHandler()
    {
        return TaskForDockerRun.class;
    }
}
