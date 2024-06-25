/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.normalizations.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;
import com.optio3.cloud.hub.engine.normalizations.value.NormalizationEngineValuePoint;

@JsonTypeName("NormalizationEngineOperatorUnaryGetPointProperty")
public class NormalizationEngineOperatorUnaryGetPointProperty extends EngineOperatorUnaryFromNormalization<EngineValuePrimitiveString, NormalizationEngineValuePoint>
{
    public NormalizationEngineValuePoint.PropertyType property;

    public NormalizationEngineOperatorUnaryGetPointProperty()
    {
        super(EngineValuePrimitiveString.class);
    }

    @Override
    protected EngineValuePrimitiveString computeResult(EngineExecutionContext<?, ?> ctx,
                                                       EngineExecutionStack stack,
                                                       NormalizationEngineValuePoint value)
    {
        return value != null ? EngineValuePrimitiveString.create(value.getProperty(property)) : null;
    }
}
