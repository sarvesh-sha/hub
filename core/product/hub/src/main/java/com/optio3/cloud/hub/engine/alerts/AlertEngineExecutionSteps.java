/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts;

import java.time.ZonedDateTime;
import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.cloud.hub.model.common.LogLine;

public class AlertEngineExecutionSteps
{
    public ZonedDateTime                  timestamp;
    public List<AlertEngineExecutionStep> steps      = Lists.newArrayList();
    public List<LogLine>                  logEntries = Lists.newArrayList();
}
