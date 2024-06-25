/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.deployment;

public class DeploymentTaskConfiguration
{
    public boolean privileged;

    public boolean useHostNetwork;

    public String entrypoint;

    public String commandLine;
}
