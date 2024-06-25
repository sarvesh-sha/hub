/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.block;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionContext;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAsset;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueLocation;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueTravelEntry;
import com.optio3.cloud.hub.logic.location.LocationsEngine;
import com.optio3.cloud.hub.model.location.LongitudeLatitude;
import com.optio3.cloud.hub.persistence.location.LocationRecord;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.util.CollectionUtils;

@JsonTypeName("AlertEngineOperatorUnaryTravelEntryInsideFence")
public class AlertEngineOperatorUnaryTravelEntryInsideFence extends EngineOperatorUnaryFromAlerts<AlertEngineValueLocation, AlertEngineValueTravelEntry>
{
    public AlertEngineOperatorUnaryTravelEntryInsideFence()
    {
        super(AlertEngineValueLocation.class);
    }

    @Override
    protected AlertEngineValueLocation computeResult(EngineExecutionContext<?, ?> ctx,
                                                     EngineExecutionStack stack,
                                                     AlertEngineValueTravelEntry entry)
    {
        if (entry != null)
        {
            AlertEngineExecutionContext ctx2     = (AlertEngineExecutionContext) ctx;
            LocationsEngine.Snapshot    snapshot = ctx2.getLocationsEngineSnapshot();

            List<TypedRecordIdentity<LocationRecord>> intersections     = snapshot.findIntersections(LongitudeLatitude.fromLngLat(entry.longitude, entry.latitude));
            TypedRecordIdentity<LocationRecord>       firstIntersection = CollectionUtils.firstElement(intersections);
            if (firstIntersection != null)
            {
                return AlertEngineValueAsset.create(ctx2, firstIntersection);
            }
        }

        return null;
    }
}
