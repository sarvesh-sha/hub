/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.metrics.value;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.engine.core.value.EngineValueEngineeringUnits;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.EngineeringUnitsFactors;
import com.optio3.serialization.Reflection;

@JsonTypeName("MetricsEngineValueScalar")
public class MetricsEngineValueScalar extends MetricsEngineValue
{
    public double                  value;
    public EngineeringUnitsFactors units;

    //--//

    public static MetricsEngineValueScalar create(double value,
                                                  EngineeringUnitsFactors units)
    {
        MetricsEngineValueScalar res = new MetricsEngineValueScalar();
        res.value = value;
        res.units = units;
        return res;
    }

    //--//

    @Override
    public int compareTo(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         EngineValue o)
    {
        MetricsEngineValueScalar other = Reflection.as(o, MetricsEngineValueScalar.class);
        if (other != null)
        {
            double thisValue  = value;
            double otherValue = other.value;

            if (units != other.units)
            {
                otherValue = EngineeringUnits.convert(otherValue, other.units, units);
            }

            return Double.compare(thisValue, otherValue);
        }

        throw stack.unexpected();
    }

    @Override
    public String format(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         Map<String, String> modifiers)
    {
        String unitsText = EngineValueEngineeringUnits.format(modifiers, units);
        if (unitsText != null)
        {
            return String.format("%f %s", value, unitsText);
        }

        return String.format("%f", value);
    }

    //--//

    public MetricsEngineValueScalar copy()
    {
        MetricsEngineValueScalar res = new MetricsEngineValueScalar();
        res.value = value;
        res.units = units;
        return res;
    }

    public MetricsEngineValueScalar convert(EngineeringUnitsFactors unitsFactorsTo)
    {
        if (unitsFactorsTo != null && unitsFactorsTo != units && unitsFactorsTo.isEquivalent(units))
        {
            MetricsEngineValueScalar converted = copy();
            converted.value = EngineeringUnits.convert(converted.value, units, unitsFactorsTo);

            return converted;
        }

        return this;
    }
}
