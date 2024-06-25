/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.value;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.persistence.location.LocationRecord;
import com.optio3.cloud.model.TypedRecordIdentity;

@JsonTypeName("AlertEngineValueLocation")
public class AlertEngineValueLocation extends AlertEngineValueAsset<LocationRecord>
{
    public static AlertEngineValueLocation createTyped(TypedRecordIdentity<LocationRecord> record)
    {
        if (!TypedRecordIdentity.isValid(record))
        {
            return null;
        }

        AlertEngineValueLocation res = new AlertEngineValueLocation();
        res.record = record;
        return res;
    }
}
