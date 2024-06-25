/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.deploy;

import com.optio3.cloud.builder.model.deployment.DeploymentStatus;
import com.optio3.cloud.builder.persistence.deployment.DeploymentAgentRecord;
import com.optio3.cloud.persistence.RecordLocator;

public abstract class BaseTaskForAgent extends BaseDeployTask
{
    public RecordLocator<DeploymentAgentRecord> loc_targetAgent;

    protected boolean setAgentStatus(DeploymentStatus status) throws
                                                              Exception
    {
        return withLocatorOrNull(loc_targetAgent, (sessionHolder, rec_agent) ->
        {
            if (rec_agent == null)
            {
                return false;
            }

            if (status == DeploymentStatus.Terminated)
            {
                rec_agent.setDockerId(null);
                rec_agent.setRpcId(null);
            }

            switch (rec_agent.getStatus())
            {
                case Cancelling:
                case Cancelled:
                    return false;

                default:
                    rec_agent.setStatus(status);

                    if (status == DeploymentStatus.Terminated)
                    {
                        sessionHolder.deleteEntity(rec_agent);
                    }

                    return true;
            }
        });
    }
}
