/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.value;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.EngineeringUnitsConverter;
import com.optio3.protocol.model.EngineeringUnitsFactors;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("EngineValuePrimitiveNumber")
public class EngineValuePrimitiveNumber extends EngineValuePrimitive
{
    public double value;

    //--//

    public static EngineValuePrimitiveNumber create(Number val)
    {
        if (val == null)
        {
            return null;
        }

        EngineValuePrimitiveNumber res = new EngineValuePrimitiveNumber();
        res.value = val.doubleValue();
        return res;
    }

    public static boolean isValidNumber(EngineValuePrimitiveNumber number)
    {
        return number != null && !Double.isNaN(number.value);
    }

    //--//

    @Override
    public String format(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         Map<String, String> modifiers)
    {
        String precision = modifiers.get("precision");
        if (StringUtils.isNotBlank(precision))
        {
            try
            {
                return String.format("%" + precision + "f", value);
            }
            catch (Exception e)
            {
                throw stack.unexpected("Invalid precision modified: %s", precision);
            }
        }

        return Double.toString(value);
    }

    @JsonIgnore
    public boolean isBoolean()
    {
        return false;
    }

    @JsonIgnore
    public boolean isString()
    {
        return false;
    }

    @JsonIgnore
    public boolean isNumber()
    {
        return true;
    }

    @JsonIgnore
    public boolean asBoolean()
    {
        return value != 0.0;
    }

    @JsonIgnore
    public String asString()
    {
        return Double.toString(value);
    }

    @JsonIgnore
    public double asNumber()
    {
        return value;
    }

    @Override
    public JsonNode extractAsJsonNode()
    {
        return DoubleNode.valueOf(value);
    }

    @Override
    public EngineValuePrimitive convertIfNeeded(EngineeringUnitsFactors sourceUnitsFactors,
                                                EngineeringUnitsFactors targetUnitsFactors)
    {
        EngineeringUnitsConverter converter = EngineeringUnits.buildConverter(sourceUnitsFactors, targetUnitsFactors);

        return create(converter.convert(value));
    }
}
