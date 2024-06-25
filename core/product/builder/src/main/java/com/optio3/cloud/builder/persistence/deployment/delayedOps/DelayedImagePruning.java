/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.deployment.delayedOps;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.builder.orchestration.tasks.deploy.TaskForImagesPruning;
import com.optio3.cloud.builder.persistence.deployment.DelayedOperation;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.serialization.Reflection;

@JsonTypeName("DelayedImagePruning")
public class DelayedImagePruning extends DelayedOperation
{
    public int daysToKeep = 30;

    public static boolean queue(RecordLocked<DeploymentHostRecord> lock_target,
                                int daysToKeep) throws
                                                Exception
    {
        DelayedImagePruning op = new DelayedImagePruning();
        op.priority   = -1000;
        op.daysToKeep = daysToKeep;

        return DeploymentHostRecord.queueUnique(lock_target, op);
    }

    //--//

    @Override
    public boolean equals(Object o)
    {
        DelayedImagePruning that = Reflection.as(o, DelayedImagePruning.class);
        if (that != null)
        {
            return true;
        }

        return false;
    }

    @Override
    public boolean mightRequireImagePull()
    {
        return false;
    }

    @Override
    public boolean validate(SessionHolder sessionHolder)
    {
        return true;
    }

    @Override
    public String getSummary(SessionHolder sessionHolder)
    {
        return String.format("Pruning images unused for more than %d days", daysToKeep);
    }

    @Override
    public NextAction process() throws
                                Exception
    {
        NextAction nextAction = shouldSleep(30);
        if (nextAction != null)
        {
            return nextAction;
        }

        DeploymentHostRecord targetHost = lock_targetHost.get();
        loggerInstance.info("Queueing delayed image pruning for host '%s'", targetHost.getDisplayName());

        return new NextAction.WaitForActivity(TaskForImagesPruning.scheduleTask(lock_targetHost, daysToKeep));
    }
}
