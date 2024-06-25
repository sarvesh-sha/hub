/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.deployment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentInstance;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;

public class DeploymentHostConfig
{
    public DeploymentInstance instanceType;

    public String imageId;

    public DeploymentRole[] roles;

    @JsonIgnore
    public boolean shouldDeployAgent()
    {
        return instanceType == null || instanceType.hasAgent;
    }
}
