/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.metrics.block;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.google.common.collect.Lists;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueSeries;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueSetOfSeries;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesInterpolate;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesPropertyResponse;
import com.optio3.protocol.model.EngineeringUnitsFactors;

@JsonSubTypes({ @JsonSubTypes.Type(value = MetricsEngineOperatorVectorBinaryAdd.class), @JsonSubTypes.Type(value = MetricsEngineOperatorVectorBinarySubtract.class) })
public abstract class MetricsEngineOperatorVectorBinary<To extends EngineValue, Ta extends EngineValue, Tb extends EngineValue> extends EngineOperatorBinaryFromMetrics<To, Ta, Tb>
{
    protected MetricsEngineOperatorVectorBinary(Class<To> resultType)
    {
        super(resultType);
    }

    //--//

    public interface CombineCallback
    {
        MetricsEngineValueSetOfSeries apply(MetricsEngineValueSetOfSeries left,
                                            EngineeringUnitsFactors leftUnits,
                                            MetricsEngineValueSetOfSeries right,
                                            EngineeringUnitsFactors rightUnits);
    }

    public static MetricsEngineValueSetOfSeries combine(double maxInterpolationGap,
                                                        MetricsEngineValueSetOfSeries left,
                                                        MetricsEngineValueSetOfSeries right,
                                                        CombineCallback callback)
    {
        left  = left.convertToSameUnits(true);
        right = right.convertToSameUnits(true);

        List<TimeSeriesPropertyResponse> dimensions = Lists.newArrayList();
        int                              lenLeft    = left.elements.size();
        int                              lenRight   = right.elements.size();
        EngineeringUnitsFactors          leftUnits  = null;
        EngineeringUnitsFactors          rightUnits = null;

        for (MetricsEngineValueSeries element : left.elements)
        {
            dimensions.add(element.values);

            if (leftUnits == null)
            {
                leftUnits = element.getUnitsFactors();
            }
        }

        for (MetricsEngineValueSeries element : right.elements)
        {
            dimensions.add(element.values);

            if (rightUnits == null)
            {
                rightUnits = element.getUnitsFactors();
            }
        }

        TimeSeriesPropertyResponse[] dimensionsInterpolated = TimeSeriesInterpolate.execute(maxInterpolationGap, dimensions).results;

        MetricsEngineValueSetOfSeries leftInterpolated = new MetricsEngineValueSetOfSeries();
        for (int i = 0; i < lenLeft; i++)
        {
            TimeSeriesPropertyResponse dimension = dimensionsInterpolated[i];
            dimension.setUnitsFactors(leftUnits);

            leftInterpolated.elements.add(new MetricsEngineValueSeries(dimension));
        }

        MetricsEngineValueSetOfSeries rightInterpolated = new MetricsEngineValueSetOfSeries();
        for (int i = 0; i < lenRight; i++)
        {
            TimeSeriesPropertyResponse dimension = dimensionsInterpolated[lenLeft + i];
            dimension.setUnitsFactors(rightUnits);

            rightInterpolated.elements.add(new MetricsEngineValueSeries(dimension));
        }

        return callback.apply(leftInterpolated, leftUnits, rightInterpolated, rightUnits);
    }
}
