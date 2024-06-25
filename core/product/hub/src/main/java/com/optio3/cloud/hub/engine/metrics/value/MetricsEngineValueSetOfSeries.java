/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.metrics.value;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.engine.core.value.EngineValueListConcrete;
import com.optio3.protocol.model.EngineeringUnitsFactors;
import com.optio3.util.Exceptions;

@JsonTypeName("MetricsEngineValueSetOfSeries")
public class MetricsEngineValueSetOfSeries extends EngineValueListConcrete<MetricsEngineValueSeries>
{
    @Override
    public int compareTo(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         EngineValue o)
    {
        throw stack.unexpected();
    }

    public MetricsEngineValueSetOfSeries convertToSameUnits(boolean throwIfIncompatible)
    {
        EngineeringUnitsFactors unitsFactorsTo = null;

        MetricsEngineValueSetOfSeries res = new MetricsEngineValueSetOfSeries();
        for (MetricsEngineValueSeries element : elements)
        {
            if (unitsFactorsTo == null)
            {
                unitsFactorsTo = element.getUnitsFactors();
                res.elements.add(element);
            }
            else
            {
                if (throwIfIncompatible && !element.canConvert(unitsFactorsTo))
                {
                    throw Exceptions.newIllegalArgumentException("The units of vector component '%s' are not compatible with '%s'", elements.indexOf(element), unitsFactorsTo);
                }

                res.elements.add(element.convert(unitsFactorsTo));
            }
        }

        return res;
    }
}
