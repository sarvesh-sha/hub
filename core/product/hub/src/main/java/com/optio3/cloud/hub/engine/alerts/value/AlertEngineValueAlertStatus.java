/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.value;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.model.alert.AlertStatus;
import com.optio3.serialization.Reflection;

@JsonTypeName("AlertEngineValueAlertStatus")
public class AlertEngineValueAlertStatus extends EngineValue
{
    public AlertStatus value;

    //--//

    public static AlertEngineValueAlertStatus create(AlertStatus val)
    {
        if (val == null)
        {
            return null;
        }

        AlertEngineValueAlertStatus res = new AlertEngineValueAlertStatus();
        res.value = val;
        return res;
    }

    public static AlertStatus extract(AlertEngineValueAlertStatus val)
    {
        return val != null ? val.value : null;
    }

    //--//

    @Override
    public int compareTo(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         EngineValue o)
    {
        AlertEngineValueAlertStatus other = Reflection.as(o, AlertEngineValueAlertStatus.class);
        if (other != null)
        {
            return value.ordinal() - other.value.ordinal();
        }

        throw stack.unexpected();
    }

    @Override
    public String format(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         Map<String, String> modifiers)
    {
        return Objects.toString(value);
    }
}
