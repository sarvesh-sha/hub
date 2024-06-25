/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.optio3.cloud.hub.engine.alerts.block.AlertEngineSampleProperty;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitive;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.protocol.model.BaseObjectModel;
import com.optio3.protocol.model.EngineeringUnitsFactors;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifier;

@JsonTypeName("AlertEngineExecutionStepSetControlPointValue")
public class AlertEngineExecutionStepSetControlPointValue extends AlertEngineExecutionStep
{
    public TypedRecordIdentity<DeviceElementRecord> controlPoint;
    public AlertEngineSampleProperty                property;
    public EngineValuePrimitive                     value;
    public EngineeringUnitsFactors                  unitsFactors;

    //--//

    @Override
    public void commit(AlertEngineExecutionContext ctx) throws
                                                        Exception
    {
        ctx.withControlPoint(controlPoint, (subSessionHolder, rec_controlPoint) ->
        {
            if (rec_controlPoint != null)
            {
                BaseObjectModel obj = rec_controlPoint.getContentsAsObject(true);
                if (obj == null)
                {
                    obj = rec_controlPoint.getContentsAsObject(false);
                }

                if (obj != null)
                {
                    Map<String, JsonNode> state = Maps.newHashMap();

                    switch (property)
                    {
                        case PresentValue:
                            JsonNode targetValue;

                            if (value != null)
                            {
                                AssetRecord.PropertyTypeExtractor extractor          = rec_controlPoint.getPropertyTypeExtractor();
                                EngineeringUnitsFactors           targetUnitsFactors = extractor.getUnitsFactors(rec_controlPoint);

                                value = value.convertIfNeeded(unitsFactors, targetUnitsFactors);

                                targetValue = value.extractAsJsonNode();
                            }
                            else
                            {
                                targetValue = null;
                            }

                            state.put(BACnetPropertyIdentifier.present_value.name(), targetValue);
                            break;
                    }

                    obj.updateState(state);

                    ObjectMapper om = obj.getObjectMapperForInstance();
                    rec_controlPoint.setDesiredContents(subSessionHolder, om, obj);
                }
            }
        });
    }
}
