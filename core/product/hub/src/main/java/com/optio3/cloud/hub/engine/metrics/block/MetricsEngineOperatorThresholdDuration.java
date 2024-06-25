/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.metrics.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueScalar;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueSeries;
import com.optio3.cloud.hub.model.shared.program.CommonEngineCompareOperation;
import com.optio3.protocol.model.EngineeringUnits;

@JsonTypeName("MetricsEngineOperatorThresholdDuration")
public class MetricsEngineOperatorThresholdDuration extends EngineOperatorBinaryFromMetrics<MetricsEngineValueSeries, MetricsEngineValueSeries, MetricsEngineValueScalar>
{
    public CommonEngineCompareOperation operation;

    public MetricsEngineOperatorThresholdDuration()
    {
        super(MetricsEngineValueSeries.class);
    }

    @Override
    protected MetricsEngineValueSeries computeResult(EngineExecutionContext<?, ?> ctx,
                                                     EngineExecutionStack stack,
                                                     MetricsEngineValueSeries series,
                                                     MetricsEngineValueScalar scalar)
    {
        stack.checkNonNullValue(series, "Missing left parameter");
        stack.checkNonNullValue(scalar, "Missing right parameter");

        double                   threshold  = EngineeringUnits.convert(scalar.value, scalar.units, series.getUnitsFactors());
        MetricsEngineValueSeries res        = series.copy();
        double[]                 timestamps = series.values.timestamps;
        double[]                 inputs     = series.values.values;
        double[]                 outputs    = res.values.values;

        double  lastTimestamp  = 0;
        boolean activePrevious = false;
        double  duration       = 0;

        for (int i = 0; i < inputs.length; i++)
        {
            boolean active;
            double  value     = inputs[i];
            double  timestamp = timestamps[i];

            switch (operation)
            {
                case LessThan:
                    active = value < threshold;
                    break;

                case LessThanOrEqual:
                    active = value <= threshold;
                    break;

                case GreaterThanOrEqual:
                    active = value >= threshold;
                    break;

                case GreaterThan:
                    active = value > threshold;
                    break;

                default:
                    throw stack.unexpected("Invalid operation: %s", operation);
            }

            if (activePrevious)
            {
                double delta = timestamp - lastTimestamp;

                duration += delta;
            }

            outputs[i] = duration;

            activePrevious = active;
            lastTimestamp  = timestamp;
        }

        res.setUnitsFactors(EngineeringUnits.seconds);

        return res;
    }
}

