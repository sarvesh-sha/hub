/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.deployer.model;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class ContainerStatus
{
    public String id;

    public String image;

    public String              name;
    public Map<String, String> labels = Maps.newHashMap();

    public List<MountPointStatus> mountPoints = Lists.newArrayList();

    public boolean running;
    public int     restartCount;
}
