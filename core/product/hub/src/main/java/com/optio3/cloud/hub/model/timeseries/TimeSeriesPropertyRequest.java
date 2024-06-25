/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.timeseries;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import com.optio3.cloud.hub.logic.samples.SamplesCache;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.protocol.model.EngineeringUnitsFactors;
import com.optio3.util.TimeUtils;

public class TimeSeriesPropertyRequest
{
    public String                  sysId;
    public String                  prop;
    public EngineeringUnitsFactors convertTo;
    public long                    offsetInSeconds;
    public boolean                 treatWideGapAsMissing;

    //--//

    public TimeSeriesPropertyResponse fetch(SamplesCache cache,
                                            TimeSeriesBaseRequest cfg,
                                            Duration maxWaitForSpooler)
    {
        TimeSeriesPropertyResponse res = new TimeSeriesPropertyResponse();

        Map<String, TimeSeriesPropertyType> properties = cache.extractClassification(sysId, false);
        res.propertySchema = AssetRecord.PropertyTypeExtractor.lookupPropertyType(properties, prop);
        if (res.propertySchema == null)
        {
            return null;
        }

        res.expectedType = res.propertySchema.getExpectedBoxedType();
        if (res.expectedType == null)
        {
            return null;
        }

        ZonedDateTime rangeStart = cfg.rangeStart;
        ZonedDateTime rangeEnd   = cfg.rangeEnd;

        if (offsetInSeconds != 0)
        {
            rangeStart = TimeUtils.plusIfNotNull(rangeStart, -offsetInSeconds, ChronoUnit.SECONDS);
            rangeEnd   = TimeUtils.plusIfNotNull(rangeEnd, -offsetInSeconds, ChronoUnit.SECONDS);
        }

        try (TimeSeriesExtract<?> extractSrc = cache.extractSamples(sysId, prop, convertTo, cfg.skipMissing, rangeStart, rangeEnd, res.expectedType, maxWaitForSpooler))
        {
            if (extractSrc.unknownProperty)
            {
                return null;
            }

            res.timeZone = extractSrc.timeZone;

            //--//

            TimeSeriesExtract<?> extractDst = extractSrc.filterSamples(cfg.maxSamples, cfg.maxGapBetweenSamples, treatWideGapAsMissing, res.propertySchema.debounceSeconds, true);
            if (extractDst == null)
            {
                extractDst = extractSrc.filterSamples(cfg.maxSamples, cfg.maxGapBetweenSamples, treatWideGapAsMissing, res.propertySchema.debounceSeconds, false);
            }

            if (!Double.isNaN(extractDst.nextTimestamp))
            {
                res.nextTimestamp = TimeUtils.fromTimestampToUtcTime(extractDst.nextTimestamp - offsetInSeconds);
            }

            res.timestamps    = extractDst.timestamps.toArray();
            res.values        = extractDst.values.toArray();
            res.enumLookup    = extractDst.extractEnumLookup();
            res.enumSetLookup = extractDst.extractEnumSetLookup();

            if (offsetInSeconds != 0)
            {
                for (int i = 0; i < res.timestamps.length; i++)
                {
                    res.timestamps[i] += offsetInSeconds;
                }
            }

            if (extractDst != extractSrc)
            {
                extractDst.close();
            }

            return res;
        }
    }
}
