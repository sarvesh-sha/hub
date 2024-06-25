/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.metrics.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineExpression;
import com.optio3.cloud.hub.engine.metrics.value.AzimuthZenithAngle;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueSeries;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesInterpolate;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesPropertyResponse;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.util.TimeUtils;

@JsonTypeName("MetricsEngineOperatorGpsSunElevation")
public class MetricsEngineOperatorGpsSunElevation extends EngineExpressionFromMetrics<MetricsEngineValueSeries>
{
    public EngineExpression<MetricsEngineValueSeries> latitude;
    public EngineExpression<MetricsEngineValueSeries> longitude;

    //--//

    public MetricsEngineOperatorGpsSunElevation()
    {
        super(MetricsEngineValueSeries.class);
    }

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack) throws
                                                             Exception
    {
        extractParams(ctx, stack, latitude, MetricsEngineValueSeries.class, longitude, MetricsEngineValueSeries.class, (latitude, longitude) ->
        {
            stack.checkNonNullValue(latitude, "Missing latitude parameter");
            stack.checkNonNullValue(longitude, "Missing longitude parameter");

            TimeSeriesPropertyResponse[] dimensionsInterpolated = TimeSeriesInterpolate.execute(60 * 60, latitude.values, longitude.values);
            double[]                     timestamps             = dimensionsInterpolated[0].timestamps;
            double[]                     latitudeRaw            = dimensionsInterpolated[0].values;
            double[]                     longitudeRaw           = dimensionsInterpolated[1].values;

            MetricsEngineValueSeries res = new MetricsEngineValueSeries(dimensionsInterpolated[0]).copy();
            res.setUnitsFactors(EngineeringUnits.degrees_angular);

            double[] outputs = res.values.values;

            for (int i = 0; i < latitudeRaw.length; i++)
            {
                double latitudeValue  = latitudeRaw[i];
                double longitudeValue = longitudeRaw[i];
                double elevation;

                if (Double.isNaN(latitudeValue) || Double.isNaN(longitudeValue))
                {
                    elevation = Double.NaN;
                }
                else
                {
                    var angles = new AzimuthZenithAngle(TimeUtils.fromTimestampToUtcTime(timestamps[i]), latitudeValue, longitudeValue);
                    elevation = 90 - angles.zenithAngle;
                }

                outputs[i] = elevation;
            }

            ctx.popBlock(res);
        });
    }
}
