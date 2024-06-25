/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.block;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionContext;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueControlPointCoordinates;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueTravelEntry;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueTravelLog;
import com.optio3.util.CollectionUtils;
import com.optio3.util.TimeUtils;

@JsonTypeName("AlertEngineOperatorUnaryCoordinatesNewSamples")
public class AlertEngineOperatorUnaryCoordinatesNewSamples extends EngineOperatorUnaryFromAlerts<AlertEngineValueTravelLog, AlertEngineValueControlPointCoordinates>
{
    public AlertEngineOperatorUnaryCoordinatesNewSamples()
    {
        super(AlertEngineValueTravelLog.class);
    }

    @Override
    protected AlertEngineValueTravelLog computeResult(EngineExecutionContext<?, ?> ctx,
                                                      EngineExecutionStack stack,
                                                      AlertEngineValueControlPointCoordinates coordinates)
    {
        AlertEngineExecutionContext                     ctx2  = (AlertEngineExecutionContext) ctx;
        AlertEngineExecutionContext.CoordinatesSnapshot cache = coordinates != null ? ctx2.getCoordinates(coordinates.latitude) : null;
        if (cache == null)
        {
            // Always return a travel log, even if empty.
            return new AlertEngineValueTravelLog();
        }

        ZonedDateTime lastTimestamp = cache.getLastSeenSample();

        // Move one second past the last seen sample.
        lastTimestamp = lastTimestamp.plus(1, ChronoUnit.SECONDS);

        AlertEngineValueTravelLog   travelLog   = cache.getTravelLog(lastTimestamp, null, Duration.of(2, ChronoUnit.SECONDS));
        AlertEngineValueTravelEntry travelEntry = CollectionUtils.lastElement(travelLog.elements);

        if (travelEntry != null)
        {
            cache.setLastSeenSample(TimeUtils.fromTimestampToUtcTime(travelEntry.timestamp));
        }
        else
        {
            cache.markControlPointAsSeen();
        }

        return travelLog;
    }
}
