/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.metrics.block;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineSelectValue;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesPropertyResponse;

@JsonTypeName("MetricsEngineCreateMultiStableSeries")
public class MetricsEngineCreateMultiStableSeries extends MetricsEngineBaseSelectValue
{
    @Override
    protected void fillValues(TimeSeriesPropertyResponse res,
                              List<MetricsEngineSelectValue> inputs)
    {
        int    numOfSamples = res.timestamps.length;
        double value        = Double.NaN;

        for (int sampleIndex = 0; sampleIndex < numOfSamples; sampleIndex++)
        {
            for (int inputIndex = 0; inputIndex < inputs.size(); inputIndex++)
            {
                MetricsEngineSelectValue selectValue = inputs.get(inputIndex);

                var input = selectValue.values.values;
                if (input[sampleIndex] > 0.0)
                {
                    // Transition to the new value at the first match.
                    value = inputIndex;
                    break;
                }
            }

            res.values[sampleIndex] = value;
        }
    }
}