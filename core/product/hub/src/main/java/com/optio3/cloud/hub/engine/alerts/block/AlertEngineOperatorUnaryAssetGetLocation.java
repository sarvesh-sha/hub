/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionContext;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAsset;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueLocation;
import com.optio3.cloud.hub.persistence.location.LocationRecord;
import com.optio3.cloud.model.TypedRecordIdentity;

@JsonTypeName("AlertEngineOperatorUnaryAssetGetLocation")
public class AlertEngineOperatorUnaryAssetGetLocation extends EngineOperatorUnaryFromAlerts<AlertEngineValueLocation, AlertEngineValueAsset<?>>
{
    public AlertEngineOperatorUnaryAssetGetLocation()
    {
        super(AlertEngineValueLocation.class);
    }

    @Override
    protected AlertEngineValueLocation computeResult(EngineExecutionContext<?, ?> ctx,
                                                     EngineExecutionStack stack,
                                                     AlertEngineValueAsset<?> asset)
    {
        AlertEngineExecutionContext ctx2 = (AlertEngineExecutionContext) ctx;

        if (asset != null)
        {
            String locationSysId = ctx2.getLocationSysId(asset.record);
            if (locationSysId != null)
            {
                return AlertEngineValueAsset.create(ctx2, TypedRecordIdentity.newTypedInstance(LocationRecord.class, locationSysId));
            }
        }

        return null;
    }
}
