/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.deployment.delayedOps;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.builder.orchestration.tasks.deploy.TaskForBootOptionsPull;
import com.optio3.cloud.builder.persistence.deployment.DelayedOperation;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.serialization.Reflection;

@JsonTypeName("DelayedBootOptionsPull")
public class DelayedBootOptionsPull extends DelayedOperation
{
    public static boolean queue(RecordLocked<DeploymentHostRecord> lock_target) throws
                                                                                Exception
    {
        DelayedBootOptionsPull op = new DelayedBootOptionsPull();
        op.priority = 200;

        return DeploymentHostRecord.queueUnique(lock_target, op);
    }

    //--//

    @Override
    public boolean equals(Object o)
    {
        DelayedBootOptionsPull that = Reflection.as(o, DelayedBootOptionsPull.class);
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
        return "Pull Boot Options";
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
        loggerInstance.info("Queueing delayed boot options pull for host '%s'", targetHost.getDisplayName());

        return new NextAction.WaitForActivity(TaskForBootOptionsPull.scheduleTask(lock_targetHost));
    }
}
