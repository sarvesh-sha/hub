/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.metrics.block;

import java.util.Set;
import java.util.function.BiConsumer;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.engine.core.block.EngineOperatorBinary;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueScalar;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueSeries;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesInterpolate;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesPropertyResponse;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.EngineeringUnitsFactors;

@JsonSubTypes({ @JsonSubTypes.Type(value = MetricsEngineOperatorBaseWithSchedule.class),
                @JsonSubTypes.Type(value = MetricsEngineOperatorBinary.class),
                @JsonSubTypes.Type(value = MetricsEngineOperatorBinaryBistable.class),
                @JsonSubTypes.Type(value = MetricsEngineOperatorBinaryLogic.class),
                @JsonSubTypes.Type(value = MetricsEngineOperatorFilter.class),
                @JsonSubTypes.Type(value = MetricsEngineOperatorThreshold.class),
                @JsonSubTypes.Type(value = MetricsEngineOperatorThresholdCount.class),
                @JsonSubTypes.Type(value = MetricsEngineOperatorThresholdDuration.class),
                @JsonSubTypes.Type(value = MetricsEngineOperatorThresholdEnum.class),
                @JsonSubTypes.Type(value = MetricsEngineOperatorThresholdPartialDuration.class),
                @JsonSubTypes.Type(value = MetricsEngineOperatorVectorBinary.class) })
public abstract class EngineOperatorBinaryFromMetrics<To extends EngineValue, Ta extends EngineValue, Tb extends EngineValue> extends EngineOperatorBinary<To, Ta, Tb>
{
    protected EngineOperatorBinaryFromMetrics(Class<To> resultType)
    {
        super(resultType);
    }

    public static MetricsEngineValueSeries renormalize(MetricsEngineValueSeries val,
                                                       EngineeringUnitsFactors unitsFactors)
    {
        EngineeringUnitsFactors simplified     = unitsFactors.simplify();
        Set<EngineeringUnits>   equivalenceSet = EngineeringUnitsFactors.getEquivalenceSet(simplified);
        if (equivalenceSet != null)
        {
            for (EngineeringUnits units : equivalenceSet)
            {
                if (EngineeringUnitsFactors.areIdentical(units.getConversionFactors(), simplified))
                {
                    //
                    // Found a well-known unit that matches perfectly with the output value.
                    // No fixup needed.
                    //
                    return val;
                }
            }
        }

        // Keep it non-normalized.
        return val.convert(unitsFactors.scaleByInverse(unitsFactors.scaling));
    }

    public static MetricsEngineValueSeries combine(double maxInterpolationGap,
                                                   MetricsEngineValueSeries left,
                                                   MetricsEngineValueSeries right,
                                                   EngineeringUnitsFactors unitsFactors,
                                                   BiConsumer<double[], double[]> callback)
    {
        TimeSeriesPropertyResponse[] args       = TimeSeriesInterpolate.execute(maxInterpolationGap, left.values, right.values);
        TimeSeriesPropertyResponse   alignLeft  = args[0];
        TimeSeriesPropertyResponse   alignRight = args[1];

        if (alignLeft.timeZone == null)
        {
            alignLeft.timeZone = alignRight.timeZone;
        }

        alignLeft.setUnitsFactors(unitsFactors);

        callback.accept(alignLeft.values, alignRight.values);

        return new MetricsEngineValueSeries(alignLeft);
    }

    public static MetricsEngineValueSeries combine(MetricsEngineValueSeries left,
                                                   MetricsEngineValueScalar right,
                                                   EngineeringUnitsFactors unitsFactors,
                                                   BiConsumer<double[], Double> callback)
    {
        MetricsEngineValueSeries res = left.copy();
        res.setUnits(unitsFactors);

        callback.accept(res.values.values, right.value);

        return renormalize(res, unitsFactors);
    }

    public static MetricsEngineValueSeries combine(MetricsEngineValueScalar left,
                                                   MetricsEngineValueSeries right,
                                                   EngineeringUnitsFactors unitsFactors,
                                                   BiConsumer<Double, double[]> callback)
    {
        MetricsEngineValueSeries res = right.copy();
        res.setUnits(unitsFactors);

        callback.accept(left.value, res.values.values);

        return renormalize(res, unitsFactors);
    }
}
