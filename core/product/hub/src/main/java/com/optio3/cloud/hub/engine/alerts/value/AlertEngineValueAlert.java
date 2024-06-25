/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.value;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionContext;
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionStep;
import com.optio3.cloud.hub.model.alert.AlertEventLevel;
import com.optio3.cloud.hub.model.alert.AlertSeverity;
import com.optio3.cloud.hub.model.alert.AlertStatus;
import com.optio3.cloud.hub.model.alert.AlertType;
import com.optio3.cloud.hub.persistence.alert.AlertRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("AlertEngineValueAlert")
public class AlertEngineValueAlert extends EngineValue implements Cloneable
{
    public TypedRecordIdentity<DeviceElementRecord> controlPoint;
    public TypedRecordIdentity<AlertRecord>         record;

    public ZonedDateTime   timestamp;
    public AlertEventLevel level;
    public AlertType       type;
    public AlertSeverity   severity;
    public AlertStatus     status;
    public String          statusText;
    public String          title;
    public String          description;
    public boolean         shouldNotify;

    @JsonIgnore
    public final List<AlertEngineExecutionStep> steps = Lists.newArrayList();

    public void linkStep(AlertEngineExecutionContext ctx,
                         AlertEngineExecutionStep step)
    {
        ctx.pushStep(step);

        steps.add(step);
    }

    public void unlinkPreviousSteps()
    {
        steps.clear();
    }

    //--//

    public AlertEngineValueAlert copy()
    {
        AlertEngineValueAlert copy = ObjectMappers.cloneThroughJson(null, this);

        for (AlertEngineExecutionStep step : steps)
        {
            copy.steps.add(ObjectMappers.cloneThroughJson(null, step));
        }

        return copy;
    }

    //--//

    @Override
    public int compareTo(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         EngineValue o)
    {
        AlertEngineValueAlert other = Reflection.as(o, AlertEngineValueAlert.class);
        if (other != null)
        {
            return StringUtils.compare(record.sysId, other.record.sysId);
        }

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
