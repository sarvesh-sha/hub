/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dashboard.panes.configuration;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.model.asset.graph.AssetGraphBinding;
import com.optio3.cloud.hub.model.dashboard.ControlPointsGroup;
import com.optio3.cloud.hub.model.dashboard.enums.AggregationTypeId;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.EngineeringUnitsFactors;

@JsonTypeName("PaneFieldConfigurationAggregatedValue")
public class PaneFieldConfigurationAggregatedValue extends PaneFieldConfiguration
{
    public ControlPointsGroup controlPointGroup;

    //--//

    // TODO: UPGRADE PATCH: Legacy fixup for field type change.
    public void setUnits(EngineeringUnits unit)
    {
        setUnitsFactors(EngineeringUnitsFactors.get(unit));
    }

    // TODO: UPGRADE PATCH: Legacy fixup for field type change.
    public void setAggregationType(AggregationTypeId aggregationType)
    {
        HubApplication.reportPatchCall(aggregationType);

        ensureControlPointsGroup();
        controlPointGroup.aggregationType      = aggregationType;
        controlPointGroup.groupAggregationType = AggregationTypeId.SUM;
    }

    // TODO: UPGRADE PATCH: Legacy fixup for field type change.
    public void setUnitsFactors(EngineeringUnitsFactors unitsFactors)
    {
        HubApplication.reportPatchCall(unitsFactors);

        ensureControlPointsGroup();
        controlPointGroup.unitsFactors = unitsFactors;
    }

    // TODO: UPGRADE PATCH: Legacy fixup for field type change.
    public void setPointInput(AssetGraphBinding pointInput)
    {
        HubApplication.reportPatchCall(pointInput);

        ensureControlPointsGroup();
        controlPointGroup.setPointInput(pointInput);
    }

    private void ensureControlPointsGroup()
    {
        if (controlPointGroup == null)
        {
            controlPointGroup = new ControlPointsGroup();
        }
    }
}
