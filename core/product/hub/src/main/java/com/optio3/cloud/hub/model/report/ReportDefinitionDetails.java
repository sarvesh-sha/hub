/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.report;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.optio3.cloud.hub.model.visualization.TimeRange;
import com.optio3.cloud.persistence.SessionHolder;

public class ReportDefinitionDetails
{
    public ReportConfiguration reportConfiguration;

    public ReportSchedulingOptions schedule;

    public void cleanUp(SessionHolder holder)
    {
        if (schedule != null)
        {
            schedule.cleanUp();
        }
    }

    @JsonIgnore
    public ZonedDateTime getNextActivation()
    {
        if (schedule != null)
        {
            return schedule.getNextReportTime();
        }

        return null;
    }

    @JsonIgnore
    public TimeRange getReportRange(ZonedDateTime reportTime)
    {
        if (schedule != null)
        {
            return schedule.getReportRange(reportTime);
        }

        return null;
    }
}
