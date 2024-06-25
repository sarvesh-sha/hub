/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.block;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionContext;
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionStepSetControlPointValue;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueControlPoint;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitive;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveBoolean;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveNumber;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesPropertyType;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.protocol.model.BaseObjectModel;
import com.optio3.protocol.model.EngineeringUnitsFactors;

@JsonTypeName("AlertEngineOperatorUnaryGetProperty")
public class AlertEngineOperatorUnaryGetProperty extends EngineOperatorUnaryFromAlerts<EngineValuePrimitive, AlertEngineValueControlPoint>
{
    public AlertEngineSampleProperty property;
    public EngineeringUnitsFactors   unitsFactors;

    //--//

    public AlertEngineOperatorUnaryGetProperty()
    {
        super(EngineValuePrimitive.class);
    }

    @Override
    protected EngineValuePrimitive computeResult(EngineExecutionContext<?, ?> ctx,
                                                 EngineExecutionStack stack,
                                                 AlertEngineValueControlPoint controlPoint)
    {
        AlertEngineExecutionContext ctx2 = (AlertEngineExecutionContext) ctx;

        var step = ctx2.findStep(AlertEngineExecutionStepSetControlPointValue.class, (oldStep) ->
        {
            if (oldStep.property == property && RecordIdentity.sameRecord(controlPoint.record, oldStep.controlPoint))
            {
                return true;
            }

            return false;
        });

        if (step != null)
        {
            return step.value.convertIfNeeded(step.unitsFactors, unitsFactors);
        }

        return ctx2.accessAsset(controlPoint.record, (rec_controlPoint) ->
        {
            if (rec_controlPoint == null)
            {
                return null;
            }

            BaseObjectModel obj;

            try
            {
                obj = rec_controlPoint.getContentsAsObject(true);
                if (obj == null)
                {
                    obj = rec_controlPoint.getContentsAsObject(false);
                }
            }
            catch (Exception e)
            {
                obj = null;
            }

            if (obj != null)
            {
                AssetRecord.PropertyTypeExtractor   extractor = rec_controlPoint.getPropertyTypeExtractor();
                Map<String, TimeSeriesPropertyType> map       = extractor.classifyRecord(rec_controlPoint, false);

                switch (property)
                {
                    case PresentValue:
                        TimeSeriesPropertyType pt = AssetRecord.PropertyTypeExtractor.lookupPropertyType(map, DeviceElementRecord.DEFAULT_PROP_NAME);

                        Object rawValue = obj.getField(pt.targetField);

                        EngineValuePrimitive value;

                        if (rawValue instanceof String)
                        {
                            value = EngineValuePrimitiveString.create((String) rawValue);
                        }
                        else if (rawValue instanceof Boolean)
                        {
                            value = EngineValuePrimitiveBoolean.create((Boolean) rawValue);
                        }
                        else if (rawValue instanceof Number)
                        {
                            value = EngineValuePrimitiveNumber.create((Number) rawValue);
                        }
                        else
                        {
                            return null;
                        }

                        value = value.convertIfNeeded(pt.unitsFactors, unitsFactors);
                        return value;
                }
            }

            return null;
        });
    }
}
