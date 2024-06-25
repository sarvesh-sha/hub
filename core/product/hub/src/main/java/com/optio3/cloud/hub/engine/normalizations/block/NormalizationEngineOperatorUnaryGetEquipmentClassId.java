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
import com.optio3.cloud.hub.engine.normalizations.value.NormalizationEngineValueEquipment;

@JsonTypeName("NormalizationEngineOperatorUnaryGetEquipmentClassId")
public class NormalizationEngineOperatorUnaryGetEquipmentClassId extends EngineOperatorUnaryFromNormalization<EngineValuePrimitiveString, NormalizationEngineValueEquipment>
{
    public NormalizationEngineOperatorUnaryGetEquipmentClassId()
    {
        super(EngineValuePrimitiveString.class);
    }

    @Override
    protected EngineValuePrimitiveString computeResult(EngineExecutionContext<?, ?> ctx,
                                                       EngineExecutionStack stack,
                                                       NormalizationEngineValueEquipment value)
    {
        return value != null ? EngineValuePrimitiveString.create(value.equipmentClassId) : null;
    }
}
