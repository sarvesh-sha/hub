/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.logging;

import java.util.Map;

import com.google.common.collect.Maps;

public class LoggerConfiguration
{
    public String parent;
    public String name;

    public Map<Severity, Boolean> levels = Maps.newHashMap();
}
