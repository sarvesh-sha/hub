/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.normalizations.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveNumber;
import com.optio3.cloud.hub.engine.normalizations.value.NormalizationEngineValueDocument;

@JsonTypeName("NormalizationEngineOperatorUnaryGetDocumentScore")
public class NormalizationEngineOperatorUnaryGetDocumentScore extends EngineOperatorUnaryFromNormalization<EngineValuePrimitiveNumber, NormalizationEngineValueDocument>
{
    public NormalizationEngineOperatorUnaryGetDocumentScore()
    {
        super(EngineValuePrimitiveNumber.class);
    }

    @Override
    protected EngineValuePrimitiveNumber computeResult(EngineExecutionContext<?, ?> ctx,
                                                       EngineExecutionStack stack,
                                                       NormalizationEngineValueDocument value)
    {
        return value != null ? EngineValuePrimitiveNumber.create(value.score) : null;
    }
}
