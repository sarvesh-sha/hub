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
import com.optio3.cloud.hub.logic.normalizations.EquipmentClass;
import com.optio3.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("NormalizationEngineOperatorUnaryGetEquipmentClassName")
public class NormalizationEngineOperatorUnaryGetEquipmentClassName extends EngineOperatorUnaryFromNormalization<EngineValuePrimitiveString, EngineValuePrimitiveString>
{
    public NormalizationEngineOperatorUnaryGetEquipmentClassName()
    {
        super(EngineValuePrimitiveString.class);
    }

    @Override
    protected EngineValuePrimitiveString computeResult(EngineExecutionContext<?, ?> ctx,
                                                       EngineExecutionStack stack,
                                                       EngineValuePrimitiveString value)
    {
        NormalizationEngineExecutionContext ctx2 = (NormalizationEngineExecutionContext) ctx;

        String         equipmentClassId = EngineValuePrimitiveString.extract(value);
        EquipmentClass equipmentClass   = equipmentClassId != null ? CollectionUtils.findFirst(ctx2.equipmentClasses, (ec) -> StringUtils.equals(ec.idAsString(), equipmentClassId)) : null;
        return equipmentClass != null ? EngineValuePrimitiveString.create(equipmentClass.equipClassName) : null;
    }
}
