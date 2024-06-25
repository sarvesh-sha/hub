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
import com.optio3.cloud.hub.engine.normalizations.NormalizationEngineExecutionContext;

@JsonTypeName("NormalizationEngineOperatorUnaryNormalize")
public class NormalizationEngineOperatorUnaryNormalize extends EngineOperatorUnaryFromNormalization<EngineValuePrimitiveString, EngineValuePrimitiveString>
{
    public NormalizationEngineOperatorUnaryNormalize()
    {
        super(EngineValuePrimitiveString.class);
    }

    @Override
    protected EngineValuePrimitiveString computeResult(EngineExecutionContext<?, ?> ctx,
                                                       EngineExecutionStack stack,
                                                       EngineValuePrimitiveString value)
    {
        NormalizationEngineExecutionContext ctx2 = (NormalizationEngineExecutionContext) ctx;

        String text       = EngineValuePrimitiveString.extract(value);
        String normalized = ctx2.normalizationEngine.normalizeSimple(text);
        return EngineValuePrimitiveString.create(normalized);
    }
}
