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
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueSetOfSeries;

@JsonTypeName("MetricsEngineOperatorVectorBinarySubtract")
public class MetricsEngineOperatorVectorBinarySubtract extends MetricsEngineOperatorVectorBinary<MetricsEngineValueSetOfSeries, MetricsEngineValueSetOfSeries, MetricsEngineValueSetOfSeries>
{
    public MetricsEngineOperatorVectorBinarySubtract()
    {
        super(MetricsEngineValueSetOfSeries.class);
    }

    @Override
    protected MetricsEngineValueSetOfSeries computeResult(EngineExecutionContext<?, ?> ctx,
                                                          EngineExecutionStack stack,
                                                          MetricsEngineValueSetOfSeries vectorA,
                                                          MetricsEngineValueSetOfSeries vectorB)
    {
        stack.checkNonNullValue(vectorA, "Missing left parameter");
        stack.checkNonNullValue(vectorB, "Missing right parameter");

        int vectorAlen = vectorA.getLength();
        int vectorBlen = vectorB.getLength();
        if (vectorAlen != vectorBlen)
        {
            throw stack.unexpected("Incompatible vectors: %d dimensions vs. %d", vectorAlen, vectorBlen);
        }

        MetricsEngineExecutionContext ctx2 = (MetricsEngineExecutionContext) ctx;

        return combine(ctx2.maxInterpolationGap, vectorA, vectorB, (left, leftUnits, right, rightUnits) ->
        {
            MetricsEngineValueSetOfSeries res = new MetricsEngineValueSetOfSeries();

            for (int v = 0; v < vectorAlen; v++)
            {
                MetricsEngineValueSeries componentLeft  = left.elements.get(v);
                MetricsEngineValueSeries componentRight = right.elements.get(v);

                componentLeft = componentLeft.copy();

                double[] valLeft  = componentLeft.values.values;
                double[] valRight = componentRight.values.values;
                for (int i = 0; i < valLeft.length; i++)
                {
                    valLeft[i] -= valRight[i];
                }

                res.elements.add(renormalize(componentLeft, leftUnits));
            }

            return res;
        });
    }
}
