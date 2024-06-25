/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.report;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.util.TimeUtils;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes({ @JsonSubTypes.Type(value = ReportScheduleDaily.class),
                @JsonSubTypes.Type(value = ReportScheduleMonthly.class),
                @JsonSubTypes.Type(value = ReportScheduleOnDemand.class),
                @JsonSubTypes.Type(value = ReportScheduleWeekly.class) })
public abstract class ReportSchedule
{
    public int hour;

    public int minute;

    public String zoneDesired;

    //--//

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setTimeOfDay(ZonedDateTime timeOfDay)
    {
        HubApplication.reportPatchCall(timeOfDay);
        m_timeOfDay = timeOfDay;
        fixupTimeOfDay();
    }

    private ZonedDateTime m_timeOfDay;

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setZoneCreated(String zoneCreated)
    {
        HubApplication.reportPatchCall(zoneCreated);
        m_zoneCreated = zoneCreated;
        fixupTimeOfDay();
    }

    private String m_zoneCreated;

    private void fixupTimeOfDay()
    {
        if (m_timeOfDay != null && m_zoneCreated != null)
        {
            ZoneId createdZone = ZoneId.of(m_zoneCreated);
            ZoneId desiredZone = ZoneId.of(zoneDesired);

            LocalTime reportTime = m_timeOfDay.withZoneSameInstant(createdZone)
                                              .withZoneSameLocal(desiredZone)
                                              .toLocalTime();

            hour   = reportTime.getHour();
            minute = reportTime.getMinute();
        }
    }

    //--//

    @JsonIgnore
    public ZonedDateTime getNextReportTime()
    {
        ZoneId desiredZone = ZoneId.of(zoneDesired);

        LocalTime nextReportTime = LocalTime.of(hour, minute);

        ZonedDateTime now = TimeUtils.now()
                                     .withZoneSameInstant(desiredZone);

        if (now.toLocalTime()
               .isAfter(nextReportTime))
        {
            now = now.plusDays(1);
        }

        ZonedDateTime nextReport = now.with(nextReportTime);

        return getNextReportTime(nextReport);
    }

    protected abstract ZonedDateTime getNextReportTime(ZonedDateTime nextReport);
}
