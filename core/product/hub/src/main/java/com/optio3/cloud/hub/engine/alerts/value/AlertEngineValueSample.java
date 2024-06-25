/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.value;

import java.time.ZonedDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.model.TypedRecordIdentity;

@JsonTypeName("AlertEngineValueSample")
public class AlertEngineValueSample extends EngineValue
{
    public TypedRecordIdentity<DeviceElementRecord> controlPoint;
    public ZonedDateTime                            timestamp;

    //--//

    public static AlertEngineValueSample create(TypedRecordIdentity<DeviceElementRecord> controlPoint,
                                                ZonedDateTime timestamp)
    {
        if (!TypedRecordIdentity.isValid(controlPoint))
        {
            return null;
        }

        AlertEngineValueSample res = new AlertEngineValueSample();
        res.controlPoint = controlPoint;
        res.timestamp    = timestamp;
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
        return null;
    }
}
