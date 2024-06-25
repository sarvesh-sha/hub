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
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.protocol.model.EngineeringUnitsFactors;

@JsonTypeName("EngineValuePrimitiveBoolean")
public class EngineValuePrimitiveBoolean extends EngineValuePrimitive
{
    public boolean value;

    //--//

    public static EngineValuePrimitiveBoolean create(boolean val)
    {
        EngineValuePrimitiveBoolean res = new EngineValuePrimitiveBoolean();
        res.value = val;
        return res;
    }

    //--//

    @Override
    public String format(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         Map<String, String> modifiers)
    {
        return Boolean.toString(value);
    }

    @JsonIgnore
    public boolean isBoolean()
    {
        return true;
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
        return value;
    }

    @JsonIgnore
    public String asString()
    {
        return Boolean.toString(value);
    }

    @JsonIgnore
    public double asNumber()
    {
        return value ? 1 : 0;
    }

    @Override
    public JsonNode extractAsJsonNode()
    {
        return BooleanNode.valueOf(value);
    }

    @Override
    public EngineValuePrimitive convertIfNeeded(EngineeringUnitsFactors sourceUnitsFactors,
                                                EngineeringUnitsFactors targetUnitsFactors)
    {
        // No-op for booleans.
        return this;
    }
}
