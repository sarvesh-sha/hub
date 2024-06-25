/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.normalizations.value;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.model.location.LocationType;

@JsonTypeName("NormalizationEngineValueLocation")
public class NormalizationEngineValueLocation extends EngineValue
{
    public String       name;
    public LocationType type;

    public static NormalizationEngineValueLocation create(String name,
                                                          LocationType type)
    {
        NormalizationEngineValueLocation res = new NormalizationEngineValueLocation();
        res.name = name;
        res.type = type;
        return res;
    }

    @Override
    public int compareTo(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         EngineValue o)
    {
        throw stack.unexpected();
    }

    @Override
    public String format(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         Map<String, String> modifiers)
    {
        return name;
    }
}
