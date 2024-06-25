/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.deployment.delayedOps;

import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Objects;
import com.optio3.cloud.builder.orchestration.tasks.deploy.TaskForAgentTermination;
import com.optio3.cloud.builder.persistence.deployment.DelayedOperation;
import com.optio3.cloud.builder.persistence.deployment.DeploymentAgentRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.serialization.Reflection;

@JsonTypeName("DelayedAgentTermination")
public class DelayedAgentTermination extends DelayedOperation
{
    public RecordLocator<DeploymentAgentRecord> loc_agent;

    //--//

    public static boolean queue(SessionHolder sessionHolder,
                                DeploymentAgentRecord rec_agent) throws
                                                                 Exception
    {
        if (rec_agent.getDockerId() == null)
        {
            return false;
        }

        if (rec_agent.isActive())
        {
            return false;
        }

        DeploymentHostRecord               rec_target  = rec_agent.getDeployment();
        RecordLocked<DeploymentHostRecord> lock_target = sessionHolder.optimisticallyUpgradeToLocked(rec_target, 2, TimeUnit.MINUTES);

        DelayedAgentTermination op = new DelayedAgentTermination();
        op.loc_agent = sessionHolder.createLocator(rec_agent);
        op.priority  = 1000;

        return DeploymentHostRecord.queueUnique(lock_target, op);
    }

    //--//

    @Override
    public boolean equals(Object o)
    {
        DelayedAgentTermination that = Reflection.as(o, DelayedAgentTermination.class);
        if (that == null)
        {
            return false;
        }

        return Objects.equal(loc_agent, that.loc_agent);
    }

    @Override
    public boolean mightRequireImagePull()
    {
        return false;
    }

    @Override
    public boolean validate(SessionHolder sessionHolder)
    {
        DeploymentAgentRecord rec_agent = sessionHolder.fromLocatorOrNull(loc_agent);
        return rec_agent != null && rec_agent.getDockerId() != null && !rec_agent.isActive();
    }

    @Override
    public String getSummary(SessionHolder sessionHolder)
    {
        DeploymentAgentRecord rec_agent = sessionHolder.fromLocator(loc_agent);
        return String.format("Terminate agent '%s'", rec_agent.getDockerId());
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
        loggerInstance.info("Queueing delayed agent termination for host '%s'", targetHost.getDisplayName());

        DeploymentAgentRecord rec_agent = sessionHolder.fromLocator(loc_agent);
        return new NextAction.WaitForActivity(TaskForAgentTermination.scheduleTask(sessionHolder, rec_agent));
    }
}
