/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.build;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Maps;
import com.optio3.cloud.builder.logic.build.BuildLogicForDockerContainer;
import com.optio3.cloud.builder.logic.build.BuildLogicForRepository;
import com.optio3.cloud.builder.orchestration.state.DockerImageState;
import com.optio3.cloud.builder.orchestration.state.DualPath;
import com.optio3.cloud.builder.persistence.jobs.JobDefinitionStepRecordForDockerBuild;
import com.optio3.cloud.builder.persistence.jobs.JobRecord;
import com.optio3.cloud.builder.persistence.jobs.JobStepRecord;
import com.optio3.cloud.builder.persistence.worker.HostRecord;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.infra.WellKnownDockerImageLabel;
import com.optio3.infra.WellKnownSites;
import com.optio3.util.Base64EncodedValue;
import com.optio3.util.Exceptions;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

public class TaskForDockerBuild extends BaseBuildTask
{
    @Override
    public String getTitle()
    {
        return "Build Docker image";
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
        HostRecord rec_host = getTargetHost(sessionHolder);
        JobRecord  rec_job  = getJob(sessionHolder);

        JobDefinitionStepRecordForDockerBuild stepDef = getCurrentStepDef(sessionHolder, JobDefinitionStepRecordForDockerBuild.class);

        //--//

        SubstitutionContext scPath = resolveVariables(stepDef.getSourcePath());
        DualPath            v      = scPath.findSource(DualPath.class, 0);
        if (v == null)
        {
            throw Exceptions.newIllegalArgumentException("No reference to virtual directory in %s", stepDef.getSourcePath());
        }

        //--//

        //
        // Build the image.
        //
        Path targetPath = Paths.get(scPath.result);
        Path relPath    = v.guestPath.relativize(targetPath);
        Path sourceDir  = v.hostPath.resolve(relPath);

        RecordLocked<JobStepRecord> lock_step = newStep(sessionHolder, "Building Docker image at '%s'", scPath.result);

        BuildLogicForDockerContainer containerLogic = new BuildLogicForDockerContainer(appConfig, sessionHolder, rec_host, rec_job);

        HashMap<String, String> labels = Maps.newHashMap();
        WellKnownDockerImageLabel.BuildId.setValue(labels, rec_job.getIdPrefix());
        WellKnownDockerImageLabel.Architecture.setValue(labels, stepDef.getArchitecture());
        WellKnownDockerImageLabel.Service.setValue(labels, stepDef.getTargetService());

        {
            BuildLogicForRepository repoLogic = new BuildLogicForRepository(appConfig, sessionHolder, rec_host, rec_job);

            final String fileName = stepDef.getTargetService() + "-deployment.yml";
            byte[]       template = repoLogic.readFile(sessionHolder.fromLocator(v.hostDir), relPath.resolve(fileName));
            if (template != null)
            {
                WellKnownDockerImageLabel.ConfigTemplate.setValue(labels, new Base64EncodedValue(template));
            }

            byte[] config = repoLogic.readFile(sessionHolder.fromLocator(v.hostDir), relPath.resolve("o3.config"));
            if (config != null)
            {
                Pattern pattern = Pattern.compile("^([A-Z0-9_]+)=(.*)");

                try
                {
                    String dbName    = null;
                    String dbLevel   = null;
                    String dbVersion = null;

                    for (String line : IOUtils.readLines(new ByteArrayInputStream(config), Charset.defaultCharset()))
                    {
                        final Matcher matcher = pattern.matcher(line);
                        if (matcher.matches())
                        {
                            String key   = matcher.group(1);
                            String value = matcher.group(2);

                            if (StringUtils.isNotEmpty(key) && StringUtils.isNotEmpty(value))
                            {
                                switch (key)
                                {
                                    case "DB_NAME":
                                        dbName = value;
                                        break;

                                    case "DB_VERSION":
                                        dbVersion = value;
                                        break;

                                    case "DB_MIGRATIONS_LEVEL":
                                        dbLevel = value;
                                        break;

                                    case "DB_MIGRATIONS_VERSION":
                                        dbVersion = value;
                                        break;
                                }
                            }
                        }
                    }

                    if (StringUtils.isNotBlank(dbName))
                    {
                        WellKnownDockerImageLabel.DatabaseName.setValue(labels, dbName);
                    }

                    if (StringUtils.isNotBlank(dbVersion))
                    {
                        if (StringUtils.isNotBlank(dbLevel))
                        {
                            dbVersion = String.format("rev%s.%s", dbLevel, dbVersion);
                        }

                        WellKnownDockerImageLabel.DatabaseSchema.setValue(labels, dbVersion);
                    }
                }
                catch (Throwable e)
                {
                    // Ignore failures.
                }
            }
        }

        DockerImageState state           = new DockerImageState();
        String           registryAddress = WellKnownSites.dockerRegistryAddress(false);
        state.imageTemporaryTag = containerLogic.buildImage(lock_step, sourceDir, stepDef.getDockerFile(), stepDef.getBuildArgs(), registryAddress, labels, appConfig.overrideBuildTime);

        putStateValue(stepDef.getBuildId(), state);

        markAsCompleted();
    }
}
