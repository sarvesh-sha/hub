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
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValue;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueScalar;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueSeries;
import com.optio3.cloud.hub.model.shared.program.CommonEngineArithmeticOperation;
import com.optio3.protocol.model.EngineeringUnitsFactors;
import com.optio3.serialization.Reflection;

@JsonTypeName("MetricsEngineOperatorBinary")
public class MetricsEngineOperatorBinary extends EngineOperatorBinaryFromMetrics<MetricsEngineValueSeries, MetricsEngineValue, MetricsEngineValue>
{
    public CommonEngineArithmeticOperation operation;

    public MetricsEngineOperatorBinary()
    {
        super(MetricsEngineValueSeries.class);
    }

    @Override
    protected MetricsEngineValueSeries computeResult(EngineExecutionContext<?, ?> ctx,
                                                     EngineExecutionStack stack,
                                                     MetricsEngineValue valueA,
                                                     MetricsEngineValue valueB)
    {
        stack.checkNonNullValue(valueA, "Missing left parameter");
        stack.checkNonNullValue(valueB, "Missing right parameter");

        MetricsEngineExecutionContext ctx2 = (MetricsEngineExecutionContext) ctx;

        MetricsEngineValueSeries seriesA = Reflection.as(valueA, MetricsEngineValueSeries.class);
        if (seriesA != null)
        {
            EngineeringUnitsFactors unitsA = seriesA.getUnitsFactors();

            MetricsEngineValueSeries seriesB = Reflection.as(valueB, MetricsEngineValueSeries.class);
            if (seriesB != null)
            {
                EngineeringUnitsFactors unitsB = seriesB.getUnitsFactors();

                switch (operation)
                {
                    case Plus:
                        return combine(ctx2.maxInterpolationGap, seriesA, seriesB.convert(unitsA), unitsA, (left, right) ->
                        {
                            for (int i = 0; i < left.length; i++)
                            {
                                left[i] += right[i];
                            }
                        });

                    case Minus:
                        return combine(ctx2.maxInterpolationGap, seriesA, seriesB.convert(unitsA), unitsA, (left, right) ->
                        {
                            for (int i = 0; i < left.length; i++)
                            {
                                left[i] -= right[i];
                            }
                        });

                    case Multiply:
                        return combine(ctx2.maxInterpolationGap, seriesA, seriesB, unitsA.multiplyBy(unitsB, false), (left, right) ->
                        {
                            for (int i = 0; i < left.length; i++)
                            {
                                left[i] *= right[i];
                            }
                        });

                    case Divide:
                        return combine(ctx2.maxInterpolationGap, seriesA, seriesB, unitsA.divideBy(unitsB, false), (left, right) ->
                        {
                            for (int i = 0; i < left.length; i++)
                            {
                                left[i] /= right[i];
                            }
                        });

                    default:
                        throw stack.unexpected();
                }
            }

            MetricsEngineValueScalar scalarB = Reflection.as(valueB, MetricsEngineValueScalar.class);
            if (scalarB != null)
            {
                EngineeringUnitsFactors unitsB = scalarB.units;

                switch (operation)
                {
                    case Plus:
                        return combine(seriesA, scalarB.convert(unitsA), unitsA, (left, right) ->
                        {
                            for (int i = 0; i < left.length; i++)
                            {
                                left[i] = left[i] + right;
                            }
                        });

                    case Minus:
                        return combine(seriesA, scalarB.convert(unitsA), unitsA, (left, right) ->
                        {
                            for (int i = 0; i < left.length; i++)
                            {
                                left[i] = left[i] - right;
                            }
                        });

                    case Multiply:
                        return combine(seriesA, scalarB, unitsA.multiplyBy(unitsB, false), (left, right) ->
                        {
                            for (int i = 0; i < left.length; i++)
                            {
                                left[i] = left[i] * right;
                            }
                        });

                    case Divide:
                        return combine(seriesA, scalarB, unitsA.divideBy(unitsB, false), (left, right) ->
                        {
                            for (int i = 0; i < left.length; i++)
                            {
                                left[i] = left[i] / right;
                            }
                        });

                    default:
                        throw stack.unexpected();
                }
            }
        }

        MetricsEngineValueScalar scalarA = Reflection.as(valueA, MetricsEngineValueScalar.class);
        if (scalarA != null)
        {
            EngineeringUnitsFactors unitsA = scalarA.units;

            MetricsEngineValueSeries seriesB = Reflection.as(valueB, MetricsEngineValueSeries.class);
            if (seriesB != null)
            {
                EngineeringUnitsFactors unitsB = seriesB.getUnitsFactors();

                switch (operation)
                {
                    case Plus:
                        return combine(scalarA.convert(unitsB), seriesB, unitsB, (left, right) ->
                        {
                            for (int i = 0; i < right.length; i++)
                            {
                                right[i] = left + right[i];
                            }
                        });

                    case Minus:
                        return combine(scalarA.convert(unitsB), seriesB, unitsB, (left, right) ->
                        {
                            for (int i = 0; i < right.length; i++)
                            {
                                right[i] = left - right[i];
                            }
                        });

                    case Multiply:
                        return combine(scalarA, seriesB, unitsA.multiplyBy(unitsB, false), (left, right) ->
                        {
                            for (int i = 0; i < right.length; i++)
                            {
                                right[i] = left * right[i];
                            }
                        });

                    case Divide:
                        return combine(scalarA, seriesB, unitsA.divideBy(unitsB, false), (left, right) ->
                        {
                            for (int i = 0; i < right.length; i++)
                            {
                                right[i] = left / right[i];
                            }
                        });

                    default:
                        throw stack.unexpected();
                }
            }
        }

        throw stack.unexpected();
    }
}

