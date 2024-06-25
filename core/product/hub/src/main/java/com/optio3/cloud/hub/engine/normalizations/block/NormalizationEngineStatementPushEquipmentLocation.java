/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.normalizations.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineExpression;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;
import com.optio3.cloud.hub.engine.normalizations.value.NormalizationEngineValueEquipment;
import com.optio3.cloud.hub.engine.normalizations.value.NormalizationEngineValueLocation;
import com.optio3.cloud.hub.model.location.LocationType;
import com.optio3.util.BoxingUtils;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("NormalizationEngineStatementPushEquipmentLocation")
public class NormalizationEngineStatementPushEquipmentLocation extends EngineStatementFromNormalization
{
    public EngineExpression<NormalizationEngineValueEquipment> equipment;

    public EngineExpression<EngineValuePrimitiveString> value;

    public LocationType type;

    //--//

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack) throws
                                                             Exception
    {
        extractParams(ctx, stack, value, EngineValuePrimitiveString.class, equipment, NormalizationEngineValueEquipment.class, (valueRaw, equipmentRaw) ->
        {
            String value = EngineValuePrimitiveString.extract(valueRaw);

            if (StringUtils.isNotEmpty(value))
            {
                if (equipmentRaw.locations == null)
                {
                    equipmentRaw.locations = Lists.newArrayList();
                }
                equipmentRaw.locations.add(NormalizationEngineValueLocation.create(value, BoxingUtils.get(type, LocationType.OTHER)));
            }

            ctx.popBlock();
        });
    }
}
