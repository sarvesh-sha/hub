/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.deployer.model;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class ImageStatus
{
    public String id;
    public String parentId;

    public long          size;
    public ZonedDateTime created;

    public List<String> repoTags = Lists.newArrayList();

    public Map<String, String> labels = Maps.newHashMap();
}
