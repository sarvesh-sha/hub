/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.deployment.delayedOps;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Objects;
import com.optio3.cloud.builder.orchestration.tasks.deploy.TaskForImagePull;
import com.optio3.cloud.builder.persistence.deployment.DelayedOperation;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryTaggedImageRecord;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.serialization.Reflection;

@JsonTypeName("DelayedImagePull")
public class DelayedImagePull extends DelayedOperation implements DelayedOperation.IPreprocessState
{
    public RecordLocator<RegistryTaggedImageRecord> loc_image;
    public String                                   targetTag;

    //--//

    public static boolean queue(RecordLocked<DeploymentHostRecord> lock_target,
                                RegistryTaggedImageRecord rec_taggedImage,
                                String targetTag) throws
                                                  Exception
    {
        SessionHolder sessionHolder = lock_target.getSessionHolder();

        if (targetTag == null)
        {
            DeploymentHostRecord rec_target = lock_target.get();

            targetTag = rec_target.computeTargetTag(rec_taggedImage);
        }

        DelayedImagePull op = new DelayedImagePull();
        op.loc_image = sessionHolder.createLocator(rec_taggedImage);
        op.targetTag = targetTag;
        op.priority  = 100;

        return DeploymentHostRecord.queueUnique(lock_target, op);
    }

    //--//

    @Override
    public boolean equals(Object o)
    {
        DelayedImagePull that = Reflection.as(o, DelayedImagePull.class);
        if (that != null)
        {
            return Objects.equal(loc_image, that.loc_image);
        }

        return false;
    }

    @Override
    public boolean mightRequireImagePull()
    {
        return true;
    }

    @Override
    public boolean validate(SessionHolder sessionHolder)
    {
        RegistryTaggedImageRecord rec_taggedImage = sessionHolder.fromLocatorOrNull(loc_image);
        return rec_taggedImage != null;
    }

    @Override
    public String getSummary(SessionHolder sessionHolder)
    {
        RegistryTaggedImageRecord rec_taggedImage = sessionHolder.fromLocator(loc_image);
        return String.format("Pull Image '%s'", rec_taggedImage.getTag());
    }

    @Override
    public NextAction process() throws
                                Exception
    {
        NextAction nextAction = shouldSleep(Math.min(retries, 30));
        if (nextAction != null)
        {
            return nextAction;
        }

        DeploymentHostRecord targetHost = lock_targetHost.get();
        loggerInstance.info("Queueing delayed image pull for host '%s'", targetHost.getDisplayName());

        RegistryTaggedImageRecord rec_taggedImage = sessionHolder.fromLocator(loc_image);

        return new NextAction.WaitForActivity(TaskForImagePull.scheduleTask(lock_targetHost, rec_taggedImage, false));
    }

    //--//

    @Override
    public void preprocessState(SessionHolder sessionHolder,
                                List<DelayedOperation> ops)
    {
        RegistryTaggedImageRecord rec_targetImage = sessionHolder.fromLocatorOrNull(loc_image);
        if (rec_targetImage != null && rec_targetImage.getTargetService() != null)
        {
            //
            // Delete pulls for images for the same role.
            //
            removeOperations(ops, DelayedImagePull.class, (op) ->
            {
                RegistryTaggedImageRecord rec_image = sessionHolder.fromLocatorOrNull(op.loc_image);
                return rec_image != null && rec_image.getTargetService() == rec_targetImage.getTargetService();
            });
        }
    }
}
