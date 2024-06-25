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
import com.fasterxml.jackson.databind.node.TextNode;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.protocol.model.EngineeringUnitsFactors;
import com.optio3.util.BoxingUtils;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("EngineValuePrimitiveString")
public class EngineValuePrimitiveString extends EngineValuePrimitive
{
    public String value;

    //--//

    public static EngineValuePrimitiveString create(String val)
    {
        if (val == null)
        {
            return null;
        }

        EngineValuePrimitiveString res = new EngineValuePrimitiveString();
        res.value = val;
        return res;
    }

    public static String extract(EngineValuePrimitiveString val)
    {
        return val != null ? val.value : null;
    }

    public static String extract(EngineValuePrimitiveString val,
                                 String defaultValue)
    {
        return BoxingUtils.get(extract(val), defaultValue);
    }

    //--//

    @Override
    public String format(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         Map<String, String> modifiers)
    {
        return value;
    }

    @JsonIgnore
    public boolean isBoolean()
    {
        return false;
    }

    @JsonIgnore
    public boolean isString()
    {
        return true;
    }

    @JsonIgnore
    public boolean isNumber()
    {
        return false;
    }

    @JsonIgnore
    public boolean asBoolean()
    {
        return StringUtils.isNotEmpty(value);
    }

    @JsonIgnore
    public String asString()
    {
        return value;
    }

    @JsonIgnore
    public double asNumber()
    {
        return Double.parseDouble(value);
    }

    @Override
    public JsonNode extractAsJsonNode()
    {
        return TextNode.valueOf(value);
    }

    @Override
    public EngineValuePrimitive convertIfNeeded(EngineeringUnitsFactors sourceUnitsFactors,
                                                EngineeringUnitsFactors targetUnitsFactors)
    {
        // No-op for strings.
        return this;
    }
}
