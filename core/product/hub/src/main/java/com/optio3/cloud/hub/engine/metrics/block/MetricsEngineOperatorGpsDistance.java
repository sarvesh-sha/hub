/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.metrics.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.mapbox.geojson.Point;
import com.mapbox.turf.TurfMeasurement;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineExpression;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueSeries;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesInterpolate;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesPropertyResponse;
import com.optio3.protocol.model.EngineeringUnits;

@JsonTypeName("MetricsEngineOperatorGpsDistance")
public class MetricsEngineOperatorGpsDistance extends EngineExpressionFromMetrics<MetricsEngineValueSeries>
{
    public EngineExpression<MetricsEngineValueSeries> latitude;
    public EngineExpression<MetricsEngineValueSeries> longitude;
    public EngineExpression<MetricsEngineValueSeries> speed;

    //--//

    public MetricsEngineOperatorGpsDistance()
    {
        super(MetricsEngineValueSeries.class);
    }

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        extractParams(ctx, stack, latitude, MetricsEngineValueSeries.class, longitude, MetricsEngineValueSeries.class, speed, MetricsEngineValueSeries.class, (latitude, longitude, speed) ->
        {
            stack.checkNonNullValue(latitude, "Missing latitude parameter");
            stack.checkNonNullValue(longitude, "Missing longitude parameter");
            stack.checkNonNullValue(speed, "Missing speed parameter");

            TimeSeriesPropertyResponse[] dimensionsInterpolated = TimeSeriesInterpolate.execute(10 * 60, latitude.values, longitude.values, speed.values);
            double[]                     latitudeRaw            = dimensionsInterpolated[0].values;
            double[]                     longitudeRaw           = dimensionsInterpolated[1].values;
            double[]                     speedRaw               = dimensionsInterpolated[2].values;

            MetricsEngineValueSeries res = new MetricsEngineValueSeries(dimensionsInterpolated[0]).copy();
            res.setUnitsFactors(EngineeringUnits.kilometers);

            double[] outputs = res.values.values;

            double distance = 0;
            Point  previous = null;

            for (int i = 0; i < latitudeRaw.length; i++)
            {
                double latitudeValue  = latitudeRaw[i];
                double longitudeValue = longitudeRaw[i];
                double speedValue     = speedRaw[i];

                if (Double.isNaN(latitudeValue) || Double.isNaN(longitudeValue) || Double.isNaN(speedValue) || speedValue < 5)
                {
                    previous = null;
                }
                else
                {
                    Point point = Point.fromLngLat(longitudeValue, latitudeValue);

                    if (previous != null)
                    {
                        distance += TurfMeasurement.distance(previous, point);
                    }

                    previous = point;
                }

                outputs[i] = distance;
            }

            ctx.popBlock(res);
        });
    }
}
