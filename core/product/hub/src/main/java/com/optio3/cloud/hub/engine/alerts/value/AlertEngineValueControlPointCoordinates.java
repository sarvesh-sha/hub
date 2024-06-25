/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.value;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.model.TypedRecordIdentity;

@JsonTypeName("AlertEngineValueControlPointCoordinates")
public class AlertEngineValueControlPointCoordinates extends EngineValue
{
    public TypedRecordIdentity<DeviceElementRecord> longitude;
    public TypedRecordIdentity<DeviceElementRecord> latitude;

    //--//

    public static AlertEngineValueControlPointCoordinates create(TypedRecordIdentity<DeviceElementRecord> longitude,
                                                                 TypedRecordIdentity<DeviceElementRecord> latitude)
    {
        if (!TypedRecordIdentity.isValid(longitude) || !TypedRecordIdentity.isValid(latitude))
        {
            return null;
        }

        AlertEngineValueControlPointCoordinates res = new AlertEngineValueControlPointCoordinates();
        res.longitude = longitude;
        res.latitude  = latitude;
        return res;
    }

    //--//

    @Override
    public int compareTo(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         EngineValue o)
    {
        throw stack.unexpected();
    }

    @Override
    public String format(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         Map<String, String> modifiers)
    {
        return null;
    }
}
