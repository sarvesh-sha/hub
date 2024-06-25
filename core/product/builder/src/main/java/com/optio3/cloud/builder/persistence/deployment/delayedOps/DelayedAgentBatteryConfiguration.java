/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.deployment.delayedOps;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.builder.orchestration.tasks.deploy.TaskForAgentBatteryConfiguration;
import com.optio3.cloud.builder.persistence.deployment.DelayedOperation;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.serialization.Reflection;

@JsonTypeName("DelayedAgentBatteryConfiguration")
public class DelayedAgentBatteryConfiguration extends DelayedOperation
{
    public static boolean queue(RecordLocked<DeploymentHostRecord> lock_target) throws
                                                                                Exception
    {
        DelayedAgentBatteryConfiguration op = new DelayedAgentBatteryConfiguration();

        return DeploymentHostRecord.queueUnique(lock_target, op);
    }

    //--//

    @Override
    public boolean equals(Object o)
    {
        DelayedAgentBatteryConfiguration that = Reflection.as(o, DelayedAgentBatteryConfiguration.class);
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
        return "Change Battery Configuration";
    }

    @Override
    public NextAction process() throws
                                Exception
    {
        NextAction nextAction = shouldSleep(5);
        if (nextAction != null)
        {
            return nextAction;
        }

        DeploymentHostRecord targetHost = lock_targetHost.get();
        loggerInstance.info("Queueing delayed battery configuration for host '%s'", targetHost.getDisplayName());

        return new NextAction.WaitForActivity(TaskForAgentBatteryConfiguration.scheduleTask(lock_targetHost));
    }
}
