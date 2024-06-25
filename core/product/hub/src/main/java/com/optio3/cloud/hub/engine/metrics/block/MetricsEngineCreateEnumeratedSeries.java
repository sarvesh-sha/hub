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

@JsonTypeName("MetricsEngineCreateEnumeratedSeries")
public class MetricsEngineCreateEnumeratedSeries extends MetricsEngineBaseSelectValue
{
    @Override
    protected void fillValues(TimeSeriesPropertyResponse res,
                              List<MetricsEngineSelectValue> inputs)
    {
        int numOfSamples = res.timestamps.length;

        for (int inputIndex = 0; inputIndex < inputs.size(); inputIndex++)
        {
            MetricsEngineSelectValue selectValue = inputs.get(inputIndex);
            double[]                 input       = selectValue.values.values;

            for (int sampleIndex = 0; sampleIndex < numOfSamples; sampleIndex++)
            {
                if (Double.isNaN(res.values[sampleIndex]) && input[sampleIndex] > 0.0)
                {
                    res.values[sampleIndex] = inputIndex;
                }
            }
        }
    }
}
