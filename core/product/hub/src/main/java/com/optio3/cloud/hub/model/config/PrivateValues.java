/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.config;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3IncludeInApiDefinitions;

@Optio3IncludeInApiDefinitions
public class PrivateValues
{
    public final List<PrivateValue> values = Lists.newArrayList();
}
