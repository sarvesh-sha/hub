/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.visualization;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

import com.google.common.collect.Maps;
import com.optio3.util.BoxingUtils;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

public class RangeSelection
{
    private static final String              c_placeholderForLocal = "local";
    private static final Map<String, String> s_legacyLookup        = Maps.newHashMap();

    static
    {
        s_legacyLookup.put("EST", "US/Eastern");
        s_legacyLookup.put("EDT", "US/Eastern");
        s_legacyLookup.put("CST", "US/Central");
        s_legacyLookup.put("CDT", "US/Central");
        s_legacyLookup.put("MST", "US/Mountain");
        s_legacyLookup.put("MDT", "US/Mountain");
        s_legacyLookup.put("PST", "US/Pacific");
        s_legacyLookup.put("PDT", "US/Pacific");
        s_legacyLookup.put("AKST", "US/Alaska");
        s_legacyLookup.put("AKDT", "US/Alaska");
        s_legacyLookup.put("HST", "US/Hawaii");
        s_legacyLookup.put("HDT", "US/Hawaii");
    }

    private String m_zone;

    public TimeRangeId   range;
    public ZonedDateTime start;
    public ZonedDateTime end;
    public String        zoneCreated;

    public static RangeSelection buildFixed(ZonedDateTime start,
                                            ZonedDateTime end)
    {
        RangeSelection rs = new RangeSelection();
        rs.start       = start;
        rs.end         = end;
        rs.zoneCreated = start.getZone()
                              .getId();
        return rs;
    }

    //--//

    public String getZone()
    {
        return m_zone;
    }

    public void setZone(String zone)
    {
        m_zone = BoxingUtils.get(s_legacyLookup.get(zone), zone);
    }

    public TimeRange resolve(ZoneId localZone,
                             boolean alignToBoundary)
    {
        return resolve(TimeUtils.now(), localZone, alignToBoundary);
    }

    public TimeRange resolve(ZonedDateTime val,
                             ZoneId localZone,
                             boolean alignToBoundary)
    {
        if (localZone == null)
        {
            localZone = ZoneId.systemDefault();
        }

        ZoneId desiredZone = resolve(m_zone);
        if (desiredZone == null)
        {
            desiredZone = localZone;
        }

        if (range == null)
        {
            TimeRange rs = new TimeRange();
            rs.start = resolveTimeWithZone(start, desiredZone, zoneCreated);
            rs.end   = resolveTimeWithZone(end, desiredZone, zoneCreated);
            return rs;
        }

        val = val.withZoneSameInstant(desiredZone);

        return range.resolve(val, alignToBoundary);
    }

    public static ZonedDateTime resolveTimeWithZone(ZonedDateTime val,
                                                    ZoneId desiredZone,
                                                    String zoneCreated)
    {
        if (val == null)
        {
            return null;
        }

        if (desiredZone == null)
        {
            desiredZone = ZoneId.systemDefault();
        }

        ZoneId created = resolve(zoneCreated);
        if (created == null)
        {
            created = ZoneId.systemDefault();
        }

        return val.withZoneSameInstant(created)
                  .withZoneSameLocal(desiredZone);
    }

    private static ZoneId resolve(String zone)
    {
        if (zone == null || StringUtils.equalsIgnoreCase(zone, c_placeholderForLocal))
        {
            return null;
        }

        try
        {
            return ZoneId.of(zone);
        }
        catch (Throwable t)
        {
            // Invalid zone.
            return null;
        }
    }
}

