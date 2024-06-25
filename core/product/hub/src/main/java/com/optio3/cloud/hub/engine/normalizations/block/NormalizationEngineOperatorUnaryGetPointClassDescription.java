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
import com.optio3.cloud.hub.logic.normalizations.PointClass;
import com.optio3.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("NormalizationEngineOperatorUnaryGetPointClassDescription")
public class NormalizationEngineOperatorUnaryGetPointClassDescription extends EngineOperatorUnaryFromNormalization<EngineValuePrimitiveString, EngineValuePrimitiveString>
{
    public NormalizationEngineOperatorUnaryGetPointClassDescription()
    {
        super(EngineValuePrimitiveString.class);
    }

    @Override
    protected EngineValuePrimitiveString computeResult(EngineExecutionContext<?, ?> ctx,
                                                       EngineExecutionStack stack,
                                                       EngineValuePrimitiveString value)
    {
        NormalizationEngineExecutionContext ctx2 = (NormalizationEngineExecutionContext) ctx;

        String     pointClassId = EngineValuePrimitiveString.extract(value);
        PointClass pointClass   = pointClassId != null ? CollectionUtils.findFirst(ctx2.pointClasses, (ec) -> StringUtils.equals(ec.idAsString(), pointClassId)) : null;
        return pointClass != null ? EngineValuePrimitiveString.create(pointClass.pointClassDescription) : null;
    }
}
