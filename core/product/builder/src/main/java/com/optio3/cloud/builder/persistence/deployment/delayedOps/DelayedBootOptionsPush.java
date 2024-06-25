/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.deployment.delayedOps;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Objects;
import com.optio3.cloud.builder.orchestration.tasks.deploy.TaskForBootOptionsPush;
import com.optio3.cloud.builder.persistence.deployment.DelayedOperation;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.infra.waypoint.BootConfig;
import com.optio3.serialization.Reflection;

@JsonTypeName("DelayedBootOptionsPush")
public class DelayedBootOptionsPush extends DelayedOperation
{
    public BootConfig.OptionAndValue optValue;

    public void setKey(BootConfig.Options key)
    {
        if (optValue == null)
        {
            optValue = new BootConfig.OptionAndValue();
        }

        optValue.key = key;
    }

    public void setValue(String value)
    {
        if (optValue == null)
        {
            optValue = new BootConfig.OptionAndValue();
        }

        optValue.value = value;
    }

    //--//

    public static boolean queue(RecordLocked<DeploymentHostRecord> lock_target,
                                BootConfig.OptionAndValue optValue) throws
                                                                    Exception
    {
        DelayedBootOptionsPush op = new DelayedBootOptionsPush();
        op.optValue = optValue;

        return DeploymentHostRecord.queueUnique(lock_target, op);
    }

    //--//

    @Override
    public boolean equals(Object o)
    {
        DelayedBootOptionsPush that = Reflection.as(o, DelayedBootOptionsPush.class);
        if (that != null)
        {
            return Objects.equal(optValue.key, that.optValue.key) && Objects.equal(optValue.keyRaw, that.optValue.keyRaw);
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
        return "Push Boot Option";
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
        loggerInstance.info("Queueing delayed boot option push for host '%s'", targetHost.getDisplayName());

        return new NextAction.WaitForActivity(TaskForBootOptionsPush.scheduleTask(lock_targetHost, optValue));
    }
}
