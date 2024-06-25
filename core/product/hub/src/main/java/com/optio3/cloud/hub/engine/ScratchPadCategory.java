/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3IncludeInApiDefinitions;

@Optio3IncludeInApiDefinitions
public class ScratchPadCategory
{
    public String id;

    public String name;

    public final List<ScratchPadEntry> entries = Lists.newArrayList();
}
