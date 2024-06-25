/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dashboard.panes.configuration;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.model.asset.graph.AssetGraphBinding;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.EngineeringUnitsFactors;

@JsonTypeName("PaneFieldConfigurationCurrentValue")
public class PaneFieldConfigurationCurrentValue extends PaneFieldConfiguration
{
    public EngineeringUnitsFactors unitsFactors;
    public String                  suffix;

    public AssetGraphBinding pointInput;

    //--//

    // TODO: UPGRADE PATCH: Legacy fixup for field type change.
    public void setUnits(EngineeringUnits unit)
    {
        HubApplication.reportPatchCall(unit);

        this.unitsFactors = EngineeringUnitsFactors.get(unit);
    }
}
