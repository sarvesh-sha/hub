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
import com.optio3.cloud.hub.model.alert.AlertSeverity;
import com.optio3.serialization.Reflection;

@JsonTypeName("AlertEngineValueAlertSeverity")
public class AlertEngineValueAlertSeverity extends EngineValue
{
    public AlertSeverity value;

    //--//

    public static AlertEngineValueAlertSeverity create(AlertSeverity val)
    {
        if (val == null)
        {
            return null;
        }

        AlertEngineValueAlertSeverity res = new AlertEngineValueAlertSeverity();
        res.value = val;
        return res;
    }

    public static AlertSeverity extract(AlertEngineValueAlertSeverity val)
    {
        return val != null ? val.value : null;
    }

    //--//

    @Override
    public int compareTo(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         EngineValue o)
    {
        AlertEngineValueAlertSeverity other = Reflection.as(o, AlertEngineValueAlertSeverity.class);
        if (other != null)
        {
            if (value.isMoreSevere(other.value))
            {
                return 1;
            }
            else if (other.value.isMoreSevere(value))
            {
                return -1;
            }
            else
            {
                return 0;
            }
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
