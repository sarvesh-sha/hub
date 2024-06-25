/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.block;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionContext;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueSample;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitive;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveBoolean;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveNumber;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.EngineeringUnitsFactors;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifier;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetStatusFlags;
import com.optio3.serialization.Reflection;

@JsonTypeName("AlertEngineOperatorUnarySampleGetProperty")
public class AlertEngineOperatorUnarySampleGetProperty extends EngineOperatorUnaryFromAlerts<EngineValuePrimitive, AlertEngineValueSample>
{
    public AlertEngineSampleProperty property;
    public EngineeringUnitsFactors   unitsFactors;

    // TODO: UPGRADE PATCH: Legacy fixup for field type change.
    public void setUnit(EngineeringUnits unit)
    {
        HubApplication.reportPatchCall(unit);

        this.unitsFactors = EngineeringUnitsFactors.get(unit);
    }

    public AlertEngineOperatorUnarySampleGetProperty()
    {
        super(EngineValuePrimitive.class);
    }

    @Override
    protected EngineValuePrimitive computeResult(EngineExecutionContext<?, ?> ctx,
                                                 EngineExecutionStack stack,
                                                 AlertEngineValueSample sample)
    {
        AlertEngineValueSample raw = stack.getNonNullValue(sample, AlertEngineValueSample.class, "null sample");

        AlertEngineExecutionContext                 ctx2    = (AlertEngineExecutionContext) ctx;
        AlertEngineExecutionContext.SamplesSnapshot cache   = ctx2.getSamples(raw.controlPoint);
        Duration                                    maxWait = Duration.of(2, ChronoUnit.SECONDS);

        switch (property)
        {
            case PresentValue:
            {
                Object val = cache.getSample(raw.timestamp, BACnetPropertyIdentifier.present_value.name(), unitsFactors, true, true, Object.class, maxWait);
                if (val != null)
                {
                    Enum<?> val_enum = Reflection.as(val, Enum.class);
                    if (val_enum != null)
                    {
                        return EngineValuePrimitiveString.create(val_enum.name());
                    }

                    if (val instanceof Boolean)
                    {
                        boolean valTyped = (boolean) val;

                        return EngineValuePrimitiveBoolean.create(valTyped);
                    }

                    Number val_num = Reflection.as(val, Number.class);
                    if (val_num != null)
                    {
                        return EngineValuePrimitiveNumber.create(val_num);
                    }

                    String val_string = Reflection.as(val, String.class);
                    if (val_string != null)
                    {
                        return EngineValuePrimitiveString.create(val_string);
                    }

                    String[] val_strings = Reflection.as(val, String[].class);
                    if (val_strings != null)
                    {
                        return EngineValuePrimitiveString.create(String.join(", ", val_strings));
                    }
                }

                return null;
            }

            case OutOfService:
            {
                Boolean val = cache.getSample(raw.timestamp, BACnetPropertyIdentifier.out_of_service.name(), null, true, true, Boolean.class, maxWait);
                if (val != null)
                {
                    return EngineValuePrimitiveBoolean.create(val);
                }

                BACnetStatusFlags val2 = cache.getSample(raw.timestamp, BACnetPropertyIdentifier.status_flags.name(), null, true, true, BACnetStatusFlags.class, maxWait);
                return EngineValuePrimitiveBoolean.create(val2 != null && val2.isSet(BACnetStatusFlags.Values.out_of_service));
            }

            case InAlarm:
            {
                BACnetStatusFlags val = cache.getSample(raw.timestamp, BACnetPropertyIdentifier.status_flags.name(), null, true, true, BACnetStatusFlags.class, maxWait);
                return EngineValuePrimitiveBoolean.create(val != null && val.isSet(BACnetStatusFlags.Values.in_alarm));
            }

            default:
                throw stack.unexpected();
        }
    }
}
