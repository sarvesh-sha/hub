/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.value;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.model.TypedRecordIdentity;

@JsonTypeName("AlertEngineValueControlPoint")
public class AlertEngineValueControlPoint extends AlertEngineValueAsset<DeviceElementRecord>
{
    public static AlertEngineValueControlPoint createTyped(TypedRecordIdentity<DeviceElementRecord> record)
    {
        if (!TypedRecordIdentity.isValid(record))
        {
            return null;
        }

        AlertEngineValueControlPoint res = new AlertEngineValueControlPoint();
        res.record = record;
        return res;
    }
}
