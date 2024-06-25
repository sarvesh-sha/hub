/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.core.value.EngineValueEngineeringUnits;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.EngineeringUnitsFactors;

@JsonTypeName("EngineLiteralEngineeringUnits")
public class EngineLiteralEngineeringUnits extends EngineLiteralFromCore<EngineValueEngineeringUnits>
{
    public EngineeringUnitsFactors unitsFactors;

    //--//

    // TODO: UPGRADE PATCH: Legacy fixup for field type change.
    public void setValue(EngineeringUnits unit)
    {
        HubApplication.reportPatchCall(unit);

        this.unitsFactors = EngineeringUnitsFactors.get(unit);
    }

    //--//

    public EngineLiteralEngineeringUnits()
    {
        super(EngineValueEngineeringUnits.class);
    }

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        ctx.popBlock(EngineValueEngineeringUnits.create(unitsFactors));
    }
}
