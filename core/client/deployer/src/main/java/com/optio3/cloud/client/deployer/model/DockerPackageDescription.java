/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.deployer.model;

import java.util.List;

import com.google.common.collect.Lists;

public class DockerPackageDescription
{
    public byte[] metadata;
    public byte[] repositories;

    public List<DockerImageDescription> images = Lists.newArrayList();
}
