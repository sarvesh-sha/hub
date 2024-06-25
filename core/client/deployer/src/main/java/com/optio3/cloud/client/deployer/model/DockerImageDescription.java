/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.deployer.model;

import java.util.Map;

import com.google.common.collect.Maps;

public class DockerImageDescription
{
    public String fileName;
    public byte[] details;

    public Map<String, String> diffIdsToLayers = Maps.newHashMap();
}
