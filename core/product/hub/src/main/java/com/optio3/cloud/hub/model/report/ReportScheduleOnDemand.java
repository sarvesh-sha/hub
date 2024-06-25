/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.report;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("ReportScheduleOnDemand")
public class ReportScheduleOnDemand extends ReportSchedule
{
    @Override
    public ZonedDateTime getNextReportTime(ZonedDateTime nextReport)
    {
        return null;
    }
}
