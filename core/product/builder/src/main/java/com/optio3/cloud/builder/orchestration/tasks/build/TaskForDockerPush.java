/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.build;

import com.optio3.cloud.builder.logic.build.BuildLogicForDockerContainer;
import com.optio3.cloud.builder.persistence.jobs.JobDefinitionStepRecordForDockerPush;
import com.optio3.cloud.builder.persistence.jobs.JobRecord;
import com.optio3.cloud.builder.persistence.jobs.JobStepRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryImageRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryTaggedImageRecord;
import com.optio3.cloud.builder.persistence.worker.HostRecord;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.infra.WellKnownSites;
import com.optio3.infra.docker.DockerImageIdentifier;
import com.optio3.infra.docker.model.Image;
import com.optio3.util.Exceptions;
import org.apache.commons.lang3.StringUtils;

public class TaskForDockerPush extends BaseBuildTask
{
    @Override
    public String getTitle()
    {
        return "Push Docker image to Registry";
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

        JobDefinitionStepRecordForDockerPush stepDef = getCurrentStepDef(sessionHolder, JobDefinitionStepRecordForDockerPush.class);

        SubstitutionContext scImage     = resolveVariables(stepDef.getSourceImage());
        String              sourceImage = scImage.result;
        if (StringUtils.isEmpty(sourceImage))
        {
            throw Exceptions.newIllegalArgumentException("No reference to Docker image in %s", stepDef.getSourceImage());
        }

        //--//

        String imageTagForPush = generateImageTag(rec_job, stepDef, true);
        String imageTagForPull = generateImageTag(rec_job, stepDef, false);

        RecordLocked<JobStepRecord> lock_step = newStep(sessionHolder, "Pushing Docker image '%s'", imageTagForPush);

        BuildLogicForDockerContainer containerLogic = new BuildLogicForDockerContainer(appConfig, sessionHolder, rec_host, rec_job);

        DockerImageArchitecture arch = containerLogic.inspectImageArchitecture(sourceImage);

        Image image;

        if (appConfig.developerSettings.dontPushImages)
        {
            containerLogic.tagImage(sourceImage, imageTagForPull);

            image = containerLogic.inspectImage(imageTagForPull);
        }
        else
        {
            image = pushImage(lock_step, containerLogic, sourceImage, imageTagForPush);
        }

        //
        // After pushing the image, track the tag and its association to the image.
        //
        RegistryImageRecord rec_newImage = RegistryImageRecord.findBySha(sessionHolder.createHelper(RegistryImageRecord.class), image.id);
        if (rec_newImage == null)
        {
            rec_newImage = RegistryImageRecord.newInstance(image.id, image.config.labels, arch);
            sessionHolder.persistEntity(rec_newImage);
        }

        RegistryTaggedImageRecord rec_newTaggedImage = RegistryTaggedImageRecord.newInstance(rec_job, rec_newImage, imageTagForPull);
        sessionHolder.persistEntity(rec_newTaggedImage);

        markAsCompleted();
    }

    private Image pushImage(RecordLocked<JobStepRecord> lock_step,
                            BuildLogicForDockerContainer containerLogic,
                            String targetImage,
                            String imageTag) throws
                                             Exception
    {
        containerLogic.tagImage(targetImage, imageTag);

        Image image = containerLogic.pushImage(lock_step, imageTag);

        containerLogic.removeImage(imageTag, true);

        return image;
    }

    private String generateImageTag(JobRecord job,
                                    JobDefinitionStepRecordForDockerPush stepDef,
                                    boolean forPush)
    {
        String imageTag = stepDef.getImageTag();

        if (forPush)
        {
            imageTag = WellKnownSites.makeDockerImageTagForPush(imageTag);
        }
        else
        {
            imageTag = WellKnownSites.makeDockerImageTagForPull(imageTag);
        }

        DockerImageIdentifier imageId = new DockerImageIdentifier(imageTag);
        imageId.tag = job.getIdPrefix();
        return imageId.getFullName();
    }
}
