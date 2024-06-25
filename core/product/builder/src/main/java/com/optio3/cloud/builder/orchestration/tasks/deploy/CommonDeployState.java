/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.deploy;

import com.optio3.cloud.builder.orchestration.AbstractBuilderActivityHandler;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.SessionHolder;

public class CommonDeployState extends AbstractBuilderActivityHandler.GenericValue
{
    public RecordLocator<CustomerServiceRecord> locatorForTargetService;

    public DeploymentRole[] targetRoles;

    public RecordLocator<DeploymentHostRecord> locatorForTargetHost;

    public String hostId;
    public String hostDisplayName;

    //--//

    public CommonDeployState()
    {
        // Jackson deserialization
    }

    public CommonDeployState(SessionHolder sessionHolder,
                             CustomerServiceRecord targetService,
                             DeploymentRole[] targetRoles,
                             DeploymentHostRecord targetHost)
    {
        this.locatorForTargetService = sessionHolder.createLocator(targetService);
        this.targetRoles             = targetRoles;
        this.locatorForTargetHost    = sessionHolder.createLocator(targetHost);

        if (targetHost != null)
        {
            this.hostId          = targetHost.getHostId();
            this.hostDisplayName = targetHost.getDisplayName();
        }
    }
}
