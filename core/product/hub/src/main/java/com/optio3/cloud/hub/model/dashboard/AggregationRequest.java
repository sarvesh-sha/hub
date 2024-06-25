/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dashboard;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.optio3.cloud.hub.logic.samples.SamplesCache;
import com.optio3.cloud.hub.logic.tags.TagsEngine;
import com.optio3.cloud.hub.model.ControlPointsSelection;
import com.optio3.cloud.hub.model.dashboard.enums.AggregationTypeId;
import com.optio3.cloud.hub.model.schedule.FilterableTimeRange;
import com.optio3.cloud.hub.model.tags.TagsJoinQuery;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesBaseRequest;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesPropertyRequest;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesPropertyResponse;
import com.optio3.cloud.hub.model.visualization.TimeRange;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.collection.ExpandableArrayOfDoubles;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.protocol.model.EngineeringUnitsFactors;
import com.optio3.serialization.ObjectMappers;
import com.optio3.service.IServiceProvider;
import com.optio3.util.BoxingUtils;
import com.optio3.util.CollectionUtils;
import com.optio3.util.Exceptions;
import com.optio3.util.TimeUtils;

public class AggregationRequest
{
    public static final Logger LoggerInstance = new Logger(AggregationRequest.class);

    public EngineeringUnitsFactors   unitsFactors;
    public AggregationTypeId         aggregationType;
    public ControlPointsSelection    selections;
    public TagsJoinQuery             query;
    public List<FilterableTimeRange> filterableRanges;
    public int                       localTimeZoneOffset;
    public String                    prop;

    public double maxInterpolationGap;

    public AggregationResponse execute(IServiceProvider serviceProvider)
    {
    	
    	System.out.println("Aggregation Request123*****");
        AggregationResponse response = new AggregationResponse();
        try { 
        if (query != null)
        {
            TagsEngine          tagsEngine   = serviceProvider.getServiceNonNull(TagsEngine.class);
            TagsEngine.Snapshot tagsSnapshot = tagsEngine.acquireSnapshot(false);

            tagsSnapshot.evaluateJoin(query, (tuple) ->
            {
                response.records.add(tuple.asSysIds());
                return null;
            });
        }
        else if (selections != null && selections.identities != null)
        {
            for (TypedRecordIdentity<DeviceElementRecord> ri : selections.identities)
            {
                response.records.add(new String[] { ri.sysId });
            }
        }

        boolean debug          = LoggerInstance.isEnabled(Severity.Debug);
        boolean debugVerbose   = LoggerInstance.isEnabled(Severity.DebugVerbose);
        boolean debugObnoxious = LoggerInstance.isEnabled(Severity.DebugObnoxious);

        if (debug)
        {
            LoggerInstance.debug("Request: %s", ObjectMappers.prettyPrintAsJson(this));
        }

        SamplesCache samplesCache = serviceProvider.getServiceNonNull(SamplesCache.class);

        for (FilterableTimeRange range : filterableRanges)
        {
            double[] results = new double[response.records.size()];
            response.resultsPerRange.add(results);
            TimeRange       tr           = range.range.resolve(ZoneOffset.ofTotalSeconds(localTimeZoneOffset * 60), true);
            List<TimeRange> timeSegments = tr.filter(range.isFilterApplied ? range.filter : null);

            if (debug)
            {
                LoggerInstance.debug("TimeRange: %s", ObjectMappers.prettyPrintAsJson(range));
                LoggerInstance.debug("TimeSegments: %s", ObjectMappers.prettyPrintAsJson(timeSegments));
            }

            TimeSeriesBaseRequest cfg = new TimeSeriesBaseRequest();

            //
            // Restrict fetched time range to the result of filtering.
            //
            TimeRange firstSegment = CollectionUtils.firstElement(timeSegments);
            TimeRange lastSegment  = CollectionUtils.lastElement(timeSegments);
            if (firstSegment != null && lastSegment != null)
            {
                cfg.rangeStart = firstSegment.start;
                cfg.rangeEnd   = lastSegment.end;

                //
                // We fetch data around the time range, so that we have samples on both sides of the range limits.
                // This allows a smooth transition if we need to interpolate the series.
                //
                cfg.rangeStart = cfg.rangeStart.minus(4, ChronoUnit.HOURS);
                cfg.rangeEnd   = cfg.rangeEnd.plus(4, ChronoUnit.HOURS);
            }

            int row = 0;

            for (String[] columns : response.records)
            {
                TimeSeriesPropertyRequest spec = new TimeSeriesPropertyRequest();
                spec.sysId                 = columns[columns.length - 1];
                spec.prop                  = BoxingUtils.get(prop, DeviceElementRecord.DEFAULT_PROP_NAME);
                spec.convertTo             = unitsFactors;
                spec.treatWideGapAsMissing = true;

                TimeSeriesPropertyResponse specResults = spec.fetch(samplesCache, cfg, Duration.of(100, ChronoUnit.MILLIS));
                if (specResults != null)
                {
                    if (debugVerbose)
                    {
                        if (specResults.timestamps != null && specResults.timestamps.length >= 2)
                        {
                            LoggerInstance.debugVerbose("Timestamps for %s: %s # %s <-> %s",
                                                        spec.sysId,
                                                        specResults.timestamps.length,
                                                        TimeUtils.fromTimestampToUtcTime(specResults.timestamps[0]),
                                                        TimeUtils.fromTimestampToUtcTime(specResults.timestamps[specResults.timestamps.length - 1]));
                        }
                        else
                        {
                            LoggerInstance.debugVerbose("Timestamps for %s: NONE", spec.sysId);
                        }
                    }

                    specResults = TimeRange.addTimestampsForTimeSegments(specResults, timeSegments, maxInterpolationGap);

                    double value;
                    double timeSum    = 0;
                    double valA       = Double.NaN;
                    double valB       = Double.NaN;
                    double timestampA = Double.NaN;
                    double timestampB = Double.NaN;

                    switch (aggregationType)
                    {
                        case NONE:
                        case FIRST:
                        case LAST:
                        case MIN:
                        case MAX:
                            value = Double.NaN;
                            break;

                        case DELTA:
                        case AVGDELTA:
                        case MEAN:
                        case SUM:
                        case INCREASE:
                        case DECREASE:
                            value = 0;
                            break;

                        default:
                            throw Exceptions.newIllegalArgumentException("Unexpected aggregation type: %s", aggregationType);
                    }

                    try (ExpandableArrayOfDoubles timestamps = ExpandableArrayOfDoubles.create(specResults.timestamps))
                    {
                        for (TimeRange timeSegment : timeSegments)
                        {
                            int posStart = locatePosition(timestamps, timeSegment.start);
                            int posEnd   = locatePosition(timestamps, timeSegment.end);

                            switch (aggregationType)
                            {
                                case SUM:
                                    while (posStart <= posEnd)
                                    {
                                        double val = specResults.values[posStart++];

                                        if (!Double.isNaN(val))
                                        {
                                            value += val;
                                        }

                                        if (debugObnoxious)
                                        {
                                            LoggerInstance.debugObnoxious("    %s: SUM %f <= %f (%s)", spec.sysId, value, val, specResults.timestamps[posStart - 1]);
                                        }
                                    }
                                    break;

                                case FIRST:
                                    while (Double.isNaN(value) && posStart <= posEnd)
                                    {
                                        value = specResults.values[posStart++];

                                        if (debugObnoxious)
                                        {
                                            LoggerInstance.debugObnoxious("    %s: FIRST %f (%s)", spec.sysId, value, specResults.timestamps[posStart - 1]);
                                        }
                                    }
                                    break;

                                case LAST:
                                    while (posStart <= posEnd)
                                    {
                                        double val = specResults.values[posEnd--];

                                        if (debugObnoxious)
                                        {
                                            LoggerInstance.debugObnoxious("    %s: LAST %f (%s)", spec.sysId, val, specResults.timestamps[posEnd + 1]);
                                        }

                                        if (!Double.isNaN(val))
                                        {
                                            value = val;
                                            break;
                                        }
                                    }
                                    break;

                                case MEAN:
                                    if (posStart < posEnd)
                                    {
                                        //
                                        // Since we are interpolating to have data on all the time intervals, the first/last samples get stretched, skewing the results.
                                        // If an interval is larger than some threshold, it's possibly artificial and should be ignored.
                                        //
                                        int    numSamples        = posEnd - posStart + 1;
                                        double timestampStart    = specResults.timestamps[posStart];
                                        double timestampEnd      = specResults.timestamps[posEnd];
                                        double averageInterval   = (timestampEnd - timestampStart) / numSamples;
                                        double intervalThreshold = Math.max(3600, 3 * averageInterval);

                                        while (posStart < posEnd)
                                        {
                                            valA = specResults.values[posStart];
                                            valB = specResults.values[posStart + 1];

                                            if (!Double.isNaN(valA) && !Double.isNaN(valB))
                                            {
                                                double timeInterval = specResults.timestamps[posStart + 1] - specResults.timestamps[posStart];

                                                if (timeInterval < intervalThreshold)
                                                {
                                                    timeSum += timeInterval;
                                                    value += (valB + valA) / 2 * timeInterval;
                                                }
                                            }

                                            if (debugObnoxious)
                                            {
                                                LoggerInstance.debugObnoxious("    %s: MEAN %f <= %f (%s) over %f (%s)",
                                                                              spec.sysId,
                                                                              value,
                                                                              valA,
                                                                              specResults.timestamps[posStart],
                                                                              valB,
                                                                              specResults.timestamps[posStart + 1]);
                                            }

                                            posStart++;
                                        }
                                    }
                                    break;

                                case MIN:
                                    while (posStart <= posEnd)
                                    {
                                        double val = specResults.values[posStart++];

                                        value = BoxingUtils.minWithNaN(val, value);

                                        if (debugObnoxious)
                                        {
                                            LoggerInstance.debugObnoxious("    %s: MIN %f <= %f (%s)", spec.sysId, value, val, specResults.timestamps[posStart - 1]);
                                        }
                                    }
                                    break;

                                case MAX:
                                    while (posStart <= posEnd)
                                    {
                                        double val = specResults.values[posStart++];

                                        value = BoxingUtils.maxWithNaN(val, value);

                                        if (debugObnoxious)
                                        {
                                            LoggerInstance.debugObnoxious("    %s: MAX %f <= %f (%s)", spec.sysId, value, val, specResults.timestamps[posStart - 1]);
                                        }
                                    }
                                    break;

                                case DELTA:
                                case AVGDELTA:
                                {
                                    if (Double.isNaN(valA))
                                    {
                                        // Look for first valid sample from the start.
                                        while (posStart < posEnd && Double.isNaN(specResults.values[posStart]))
                                        {
                                            posStart++;
                                        }

                                        valA       = specResults.values[posStart];
                                        timestampA = specResults.timestamps[posStart];
                                    }

                                    // Look for first valid sample from the end.
                                    while (posStart < posEnd && Double.isNaN(specResults.values[posEnd]))
                                    {
                                        posEnd--;
                                    }

                                    valB       = specResults.values[posEnd];
                                    timestampB = specResults.timestamps[posEnd];
                                    break;
                                }

                                case INCREASE:
                                case DECREASE:
                                {
                                    boolean onlyPositive = aggregationType == AggregationTypeId.INCREASE;
                                    double  valPrevious  = Double.NaN;

                                    while (posStart <= posEnd)
                                    {
                                        double val = specResults.values[posStart++];

                                        if (!Double.isNaN(val))
                                        {
                                            if (!Double.isNaN(valPrevious))
                                            {
                                                double delta = val - valPrevious;

                                                if (onlyPositive)
                                                {
                                                    if (delta > 0)
                                                    {
                                                        value += delta;
                                                    }
                                                }
                                                else
                                                {
                                                    if (delta < 0)
                                                    {
                                                        value -= delta; // We accumulate the absolute delta.
                                                    }
                                                }

                                                if (debugObnoxious)
                                                {
                                                    LoggerInstance.debugObnoxious("    %s: %s %f <= %f (%s)", spec.sysId, aggregationType, value, val, specResults.timestamps[posStart - 1]);
                                                }
                                            }

                                            valPrevious = val;
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    }

                    switch (aggregationType)
                    {
                        case MEAN:
                            value = timeSum > 0 ? value / timeSum : Double.NaN;
                            break;

                        case DELTA:
                        case AVGDELTA: // It's just a UI thing, SUM vs MEAN in the aggregation groups.
                            if (!Double.isNaN(valA) && !Double.isNaN(valB))
                            {
                                value = valB - valA;
                            }

                            if (debugObnoxious)
                            {
                                LoggerInstance.debugObnoxious("    %s: %s %f <= %f (%s) over %f (%s)", spec.sysId, aggregationType, value, valA, timestampA, valB, timestampB);
                            }
                    }

                    results[row] = value;
                }
                else
                {
                    results[row] = Double.NaN;
                }

                if (debugObnoxious)
                {
                    LoggerInstance.debugObnoxious("");
                }

                row++;
            }
        }
    	}
    	catch (Exception e) {
    		System.out.println("Aggregation Request Exceptiion*****" + e.getMessage());
			e.printStackTrace();
			throw e;
		}
        return response;
    }

    private static int locatePosition(ExpandableArrayOfDoubles timestamps,
                                      ZonedDateTime val)
    {
        int pos = timestamps.binarySearch(val.toEpochSecond());

        if (pos < 0)
        {
            pos = ~pos;
        }

        return Math.min(pos, timestamps.size() - 1);
    }
}
