/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.admin;

import java.util.List;

import com.google.common.collect.Lists;

public class HubUniqueStackTrace
{
    public final List<String> threads = Lists.newArrayList();
    public final List<String> frames  = Lists.newArrayList();
}
