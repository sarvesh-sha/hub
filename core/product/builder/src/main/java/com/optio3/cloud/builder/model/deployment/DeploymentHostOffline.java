/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.deployment;

import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;

public class DeploymentHostOffline
{
    public String repoImage;
    public String repoAddress;
    public String repoUser;
    public String repoPassword;

    public DeploymentRole role;
    public String         containerConfig;
}
