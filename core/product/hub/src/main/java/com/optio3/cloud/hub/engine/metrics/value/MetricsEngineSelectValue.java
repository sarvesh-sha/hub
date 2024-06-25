/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.metrics.value;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesPropertyResponse;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("MetricsEngineSelectValue")
public class MetricsEngineSelectValue extends EngineValue
{
    public String                     identifier;
    public TimeSeriesPropertyResponse values;

    public static MetricsEngineSelectValue create(String id,
                                                  MetricsEngineValueSeries series)
    {
        if (StringUtils.isBlank(id) || series == null)
        {
            return null;
        }

        var res = new MetricsEngineSelectValue();
        res.identifier = id;
        res.values     = series.values;
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
