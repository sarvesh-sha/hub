/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.value;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.util.TimeUtils;

@JsonTypeName("AlertEngineValueTravelEntry")
public class AlertEngineValueTravelEntry extends EngineValue
{
    public double timestamp;
    public double longitude;
    public double latitude;

    //--//

    public static AlertEngineValueTravelEntry create(double timestamp,
                                                     double longitude,
                                                     double latitude)
    {
        AlertEngineValueTravelEntry res = new AlertEngineValueTravelEntry();
        res.timestamp = timestamp;
        res.longitude = longitude;
        res.latitude  = latitude;
        return res;
    }

    //--//

    @Override
    public int compareTo(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         EngineValue o)
    {
        throw stack.unexpected();
    }

    @Override
    public String format(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         Map<String, String> modifiers)
    {
        return String.format("%s, %s at %s", latitude, longitude, TimeUtils.fromTimestampToUtcTime(timestamp));
    }
}
