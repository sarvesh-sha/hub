/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.value;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.model.TypedRecordIdentity;

@JsonTypeName("AlertEngineValueDevice")
public class AlertEngineValueDevice extends AlertEngineValueAsset<DeviceRecord>
{
    public static AlertEngineValueDevice createTyped(TypedRecordIdentity<DeviceRecord> record)
    {
        if (!TypedRecordIdentity.isValid(record))
        {
            return null;
        }

        AlertEngineValueDevice res = new AlertEngineValueDevice();
        res.record = record;
        return res;
    }
}
