/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.normalizations.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineExpression;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveNumber;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;
import com.optio3.cloud.hub.engine.normalizations.NormalizationEngineExecutionContext;
import com.optio3.cloud.persistence.MetadataMap;
import com.optio3.serialization.Reflection;

@JsonTypeName("NormalizationEngineStatementSetMetadata")
public class NormalizationEngineStatementSetMetadata extends EngineStatementFromNormalization
{
    public String key;

    public EngineExpression<EngineValue> value;

    //--//

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack) throws
                                                             Exception
    {
        extractParams(ctx, stack, value, EngineValue.class, (value) ->
        {
            NormalizationEngineExecutionContext ctx2     = (NormalizationEngineExecutionContext) ctx;
            MetadataMap                         metadata = ctx2.state.metadata;

            metadata.remove(key);

            assign(value, metadata);

            ctx.popBlock();
        });
    }

    private void assign(EngineValue value,
                        MetadataMap metadata)
    {
        EngineValuePrimitiveString valueString = Reflection.as(value, EngineValuePrimitiveString.class);
        if (valueString != null)
        {
            metadata.putString(key, valueString.value);
            return;
        }

        EngineValuePrimitiveNumber valueNumber = Reflection.as(value, EngineValuePrimitiveNumber.class);
        if (valueNumber != null)
        {
            metadata.putDouble(key, valueNumber.value);
            return;
        }
    }
}
