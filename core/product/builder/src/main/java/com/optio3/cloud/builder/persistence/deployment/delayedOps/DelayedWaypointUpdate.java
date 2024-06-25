/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.deployment.delayedOps;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Objects;
import com.optio3.cloud.builder.orchestration.tasks.deploy.TaskForWaypointUpdate;
import com.optio3.cloud.builder.persistence.deployment.DelayedOperation;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryTaggedImageRecord;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.serialization.Reflection;

@JsonTypeName("DelayedWaypointUpdate")
public class DelayedWaypointUpdate extends DelayedOperation
{
    public RecordLocator<RegistryTaggedImageRecord> loc_image;

    //--//

    public static boolean queue(RecordLocked<DeploymentHostRecord> lock_target,
                                RegistryTaggedImageRecord rec_taggedImage) throws
                                                                           Exception
    {
        SessionHolder sessionHolder = lock_target.getSessionHolder();

        DelayedWaypointUpdate op = new DelayedWaypointUpdate();
        op.loc_image = sessionHolder.createLocator(rec_taggedImage);

        return DeploymentHostRecord.queueUnique(lock_target, op);
    }

    //--//

    @Override
    public boolean equals(Object o)
    {
        DelayedWaypointUpdate that = Reflection.as(o, DelayedWaypointUpdate.class);
        if (that == null)
        {
            return false;
        }

        return Objects.equal(loc_image, that.loc_image);
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
        return String.format("Update Waypoint with image '%s'", rec_taggedImage.getTag());
    }

    @Override
    public NextAction process() throws
                                Exception
    {
        NextAction nextAction = shouldSleep(10);
        if (nextAction != null)
        {
            return nextAction;
        }

        DeploymentHostRecord targetHost = lock_targetHost.get();
        loggerInstance.info("Queueing delayed waypoint update for host '%s'", targetHost.getDisplayName());

        RegistryTaggedImageRecord rec_taggedImage = sessionHolder.fromLocator(loc_image);

        return new NextAction.WaitForActivity(TaskForWaypointUpdate.scheduleTask(lock_targetHost, rec_taggedImage));
    }
}
