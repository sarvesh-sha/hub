/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.alert;

import java.time.ZonedDateTime;
import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionStepsOverRange;
import com.optio3.cloud.hub.model.common.LogLine;
import com.optio3.cloud.model.scheduler.BaseBackgroundActivityProgress;

public class AlertTestProgress extends BaseBackgroundActivityProgress
{
    public ZonedDateTime start;
    public ZonedDateTime end;
    public ZonedDateTime current;

    public AlertEngineExecutionStepsOverRange results;
    public List<LogLine>                      logEntries = Lists.newArrayList();
}

