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
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;
import com.optio3.cloud.hub.engine.normalizations.NormalizationEngineExecutionContext;
import com.optio3.cloud.hub.engine.normalizations.value.NormalizationEngineValueLocation;
import com.optio3.cloud.hub.model.location.LocationType;
import com.optio3.util.BoxingUtils;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("NormalizationEngineStatementPushLocation")
public class NormalizationEngineStatementPushLocation extends EngineStatementFromNormalization
{
    public EngineExpression<EngineValuePrimitiveString> value;

    public LocationType type;

    //--//

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack) throws
                                                             Exception
    {
        extractParams(ctx, stack, value, EngineValuePrimitiveString.class, (valueRaw) ->
        {
            String value = EngineValuePrimitiveString.extract(valueRaw);

            NormalizationEngineExecutionContext ctx2 = (NormalizationEngineExecutionContext) ctx;
            if (StringUtils.isNotEmpty(value))
            {
                ctx2.state.locations.add(NormalizationEngineValueLocation.create(value, BoxingUtils.get(type, LocationType.OTHER)));
            }

            ctx.popBlock();
        });
    }
}
