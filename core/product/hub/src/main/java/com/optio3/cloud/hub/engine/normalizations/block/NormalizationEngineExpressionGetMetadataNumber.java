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
import com.optio3.cloud.hub.engine.normalizations.NormalizationEngineExecutionContext;
import com.optio3.cloud.persistence.MetadataMap;

@JsonTypeName("NormalizationEngineExpressionGetMetadataNumber")
public class NormalizationEngineExpressionGetMetadataNumber extends EngineExpressionFromNormalization<EngineValuePrimitiveNumber>
{
    public String key;

    //--//

    public NormalizationEngineExpressionGetMetadataNumber()
    {
        super(EngineValuePrimitiveNumber.class);
    }

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        NormalizationEngineExecutionContext ctx2     = (NormalizationEngineExecutionContext) ctx;
        MetadataMap                         metadata = ctx2.state.metadata;

        ctx.popBlock(EngineValuePrimitiveNumber.create(metadata.getDoubleOrDefault(key, 0.0)));
    }
}
