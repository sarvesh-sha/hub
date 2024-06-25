/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.normalizations.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.core.value.EngineValueList;
import com.optio3.cloud.hub.engine.core.value.EngineValueListConcrete;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;
import com.optio3.cloud.hub.engine.normalizations.NormalizationEngineExecutionContext;
import com.optio3.cloud.hub.engine.normalizations.value.NormalizationEngineValueEquipment;
import com.optio3.util.CollectionUtils;

@JsonTypeName("NormalizationEngineExpressionGetEquipments")
public class NormalizationEngineExpressionGetEquipments extends EngineExpressionFromNormalization<EngineValueList<EngineValuePrimitiveString>>
{
    public NormalizationEngineExpressionGetEquipments()
    {
        super(EngineValueList.typeRef_ListOfStrings);
    }

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        NormalizationEngineExecutionContext                 ctx2 = (NormalizationEngineExecutionContext) ctx;
        EngineValueListConcrete<EngineValuePrimitiveString> res  = new EngineValueListConcrete<>();

        for (NormalizationEngineValueEquipment firstEquip = CollectionUtils.firstElement(ctx2.state.equipments); firstEquip != null; firstEquip = CollectionUtils.firstElement(firstEquip.childEquipment))
        {
            res.elements.add(EngineValuePrimitiveString.create(firstEquip.name));
        }

        ctx.popBlock(res);
    }
}