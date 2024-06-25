/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.value;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.EngineeringUnitsFactors;

@JsonTypeName("EngineValueEngineeringUnits")
public class EngineValueEngineeringUnits extends EngineValue
{
    public EngineeringUnitsFactors value;

    //--//

    public static EngineValueEngineeringUnits create(EngineeringUnitsFactors val)
    {
        if (val == null)
        {
            return null;
        }

        EngineValueEngineeringUnits res = new EngineValueEngineeringUnits();
        res.value = val;
        return res;
    }

    public static EngineeringUnitsFactors extract(EngineValueEngineeringUnits val)
    {
        return val != null ? val.value : null;
    }

    //--//

    @Override
    public int compareTo(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         EngineValue o)
    {
        if (o instanceof EngineValueEngineeringUnits)
        {
            EngineeringUnitsFactors otherValue = ((EngineValueEngineeringUnits) o).value;
            if (otherValue == value)
            {
                return 0;
            }

            if (otherValue != null && value != null)
            {
                return value.isEquivalent(otherValue) ? 0 : 1;
            }

            return 1;
        }

        throw stack.unexpected();
    }

    @Override
    public String format(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         Map<String, String> modifiers)
    {
        return format(modifiers, value);
    }

    public static String format(Map<String, String> modifiers,
                                EngineeringUnitsFactors value)
    {
        if (value != null)
        {
            EngineeringUnits unit = value.getPrimary();
            if (unit != null)
            {
                return unit.getDisplayName();
            }
        }

        return null;
    }
}
