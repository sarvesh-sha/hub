/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.build;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.optio3.cloud.builder.logic.build.BuildLogicForMaven;
import com.optio3.cloud.builder.orchestration.state.DualPath;
import com.optio3.cloud.builder.orchestration.state.MavenState;
import com.optio3.cloud.builder.orchestration.state.RepositoryState;
import com.optio3.cloud.builder.persistence.jobs.JobDefinitionStepRecordForMaven;
import com.optio3.cloud.builder.persistence.jobs.JobRecord;
import com.optio3.cloud.builder.persistence.worker.HostRecord;
import com.optio3.cloud.builder.persistence.worker.ManagedDirectoryRecord;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.util.IdGenerator;
import org.apache.maven.model.Model;

public class TaskForMaven extends BaseBuildTask
{
    @Override
    public String getTitle()
    {
        return "Configure Maven";
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

        JobDefinitionStepRecordForMaven stepDef = getCurrentStepDef(sessionHolder, JobDefinitionStepRecordForMaven.class);

        //
        // Create local directory for Maven repository
        //
        String prefix = IdGenerator.newGuid();
        Path   root   = Paths.get(appConfig.managedDirectoriesRoot);
        Path   dir    = root.resolve("local_maven_repo-" + prefix);

        ManagedDirectoryRecord rec_localRepo = ManagedDirectoryRecord.newInstance(rec_host, dir);
        rec_localRepo.setDeleteOnRelease(true);
        sessionHolder.persistEntity(rec_localRepo);
        rec_localRepo.acquire(rec_job);

        MavenState state = new MavenState();
        state.dir = DualPath.newInstance(sessionHolder, rec_localRepo, stepDef.getDirectory());
        putStateValue(stepDef.getBuildId(), state);

        //--//

        rec_localRepo.createDirectory(hostRemoter);

        BuildLogicForMaven mavenLogic = new BuildLogicForMaven(appConfig, rec_host, rec_job);
        mavenLogic.createSettingsForNexus(rec_localRepo, null, getNexusRepo(stepDef.getPullFrom()));

        if (appConfig.developerSettings.useLocalhostAsNexus)
        {
            for (RepositoryState stateGit : getStateValues(RepositoryState.class))
            {
                ManagedDirectoryRecord rec = sessionHolder.fromLocator(stateGit.dir.hostDir);

                Model pom = mavenLogic.loadPom(rec, null);

                pom.getDistributionManagement()
                   .getRepository()
                   .setUrl(getNexusRepoForReleases());
                pom.getDistributionManagement()
                   .getSnapshotRepository()
                   .setUrl(getNexusRepoForSnapshots());

                mavenLogic.savePom(rec, null, pom);
            }
        }

        markAsCompleted();
    }
}
