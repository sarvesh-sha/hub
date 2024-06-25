/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.databind.JsonNode;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.protocol.model.EngineeringUnitsFactors;
import com.optio3.serialization.Reflection;
import org.apache.commons.lang3.StringUtils;

@JsonSubTypes(
        { @JsonSubTypes.Type(value = EngineValuePrimitiveBoolean.class), @JsonSubTypes.Type(value = EngineValuePrimitiveString.class), @JsonSubTypes.Type(value = EngineValuePrimitiveNumber.class) })
public abstract class EngineValuePrimitive extends EngineValue
{
    @JsonIgnore
    public abstract boolean isBoolean();

    @JsonIgnore
    public abstract boolean isString();

    @JsonIgnore
    public abstract boolean isNumber();

    @JsonIgnore
    public abstract boolean asBoolean();

    @JsonIgnore
    public abstract String asString();

    @JsonIgnore
    public abstract double asNumber();

    public abstract JsonNode extractAsJsonNode();

    public abstract EngineValuePrimitive convertIfNeeded(EngineeringUnitsFactors sourceUnitsFactors,
                                                         EngineeringUnitsFactors targetUnitsFactors);

    //--//

    @Override
    public int compareTo(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         EngineValue o)
    {
        EngineValuePrimitive other = Reflection.as(o, EngineValuePrimitive.class);
        if (other != null)
        {
            if (isString() || other.isString())
            {
                return StringUtils.compare(asString(), other.asString());
            }

            return Double.compare(asNumber(), other.asNumber());
        }

        throw stack.unexpected();
    }

    public static boolean isTrue(EngineValue o)
    {
        EngineValuePrimitive val = Reflection.as(o, EngineValuePrimitive.class);
        if (val != null)
        {
            return val.asBoolean();
        }

        return o != null;
    }
}
