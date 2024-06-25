/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.metrics.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.metrics.MetricsEngineExecutionContext;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueSeries;
import com.optio3.cloud.hub.model.shared.program.CommonEngineLogicOperation;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.EngineeringUnitsFactors;

@JsonTypeName("MetricsEngineOperatorBinaryLogic")
public class MetricsEngineOperatorBinaryLogic extends EngineOperatorBinaryFromMetrics<MetricsEngineValueSeries, MetricsEngineValueSeries, MetricsEngineValueSeries>
{
    public CommonEngineLogicOperation operation;

    public MetricsEngineOperatorBinaryLogic()
    {
        super(MetricsEngineValueSeries.class);
    }

    @Override
    protected MetricsEngineValueSeries computeResult(EngineExecutionContext<?, ?> ctx,
                                                     EngineExecutionStack stack,
                                                     MetricsEngineValueSeries seriesA,
                                                     MetricsEngineValueSeries seriesB)
    {
        stack.checkNonNullValue(seriesA, "Missing left parameter");
        stack.checkNonNullValue(seriesB, "Missing right parameter");

        MetricsEngineExecutionContext ctx2 = (MetricsEngineExecutionContext) ctx;

        EngineeringUnitsFactors units = EngineeringUnits.activeInactive.getConversionFactors();

        if (operation == CommonEngineLogicOperation.Or)
        {
            return combine(ctx2.maxInterpolationGap, seriesA, seriesB, units, (left, right) ->
            {
                for (int i = 0; i < left.length; i++)
                {
                    left[i] = ((left[i] > 0.0) || (right[i] > 0.0)) ? 1 : 0;
                }
            });
        }

        if (operation == CommonEngineLogicOperation.NotOr)
        {
            return combine(ctx2.maxInterpolationGap, seriesA, seriesB, units, (left, right) ->
            {
                for (int i = 0; i < left.length; i++)
                {
                    left[i] = ((left[i] > 0.0) || (right[i] > 0.0)) ? 0 : 1;
                }
            });
        }

        if (operation == CommonEngineLogicOperation.And)
        {
            return combine(ctx2.maxInterpolationGap, seriesA, seriesB, units, (left, right) ->
            {
                for (int i = 0; i < left.length; i++)
                {
                    left[i] = ((left[i] > 0.0) && (right[i] > 0.0)) ? 1 : 0;
                }
            });
        }

        if (operation == CommonEngineLogicOperation.NotAnd)
        {
            return combine(ctx2.maxInterpolationGap, seriesA, seriesB, units, (left, right) ->
            {
                for (int i = 0; i < left.length; i++)
                {
                    left[i] = ((left[i] > 0.0) && (right[i] > 0.0)) ? 0 : 1;
                }
            });
        }

        if (operation == CommonEngineLogicOperation.Xor)
        {
            return combine(ctx2.maxInterpolationGap, seriesA, seriesB, units, (left, right) ->
            {
                for (int i = 0; i < left.length; i++)
                {
                    left[i] = ((left[i] > 0.0) ^ (right[i] > 0.0)) ? 1 : 0;
                }
            });
        }

        if (operation == CommonEngineLogicOperation.NotXor)
        {
            return combine(ctx2.maxInterpolationGap, seriesA, seriesB, units, (left, right) ->
            {
                for (int i = 0; i < left.length; i++)
                {
                    left[i] = ((left[i] > 0.0) ^ (right[i] > 0.0)) ? 0 : 1;
                }
            });
        }

        throw stack.unexpected("Unexpected operation '%s'", operation);
    }
}
