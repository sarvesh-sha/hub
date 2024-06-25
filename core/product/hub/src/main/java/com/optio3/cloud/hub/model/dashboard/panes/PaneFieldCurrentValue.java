/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dashboard.panes;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.EngineeringUnitsFactors;

@JsonTypeName("PaneFieldCurrentValue")
public class PaneFieldCurrentValue extends PaneField
{
    public TypedRecordIdentity<DeviceElementRecord> value;
    public EngineeringUnitsFactors                  unitsFactors;
    public String                                   suffix;

    //--//

    // TODO: UPGRADE PATCH: Legacy fixup for field type change.
    public void setUnits(EngineeringUnits unit)
    {
        HubApplication.reportPatchCall(unit);

        this.unitsFactors = EngineeringUnitsFactors.get(unit);
    }

    public static PaneFieldCurrentValue create(String label,
                                               TypedRecordIdentity<DeviceElementRecord> point,
                                               EngineeringUnitsFactors unitsFactors,
                                               String suffix)
    {
        if (point == null)
        {
            return null;
        }

        PaneFieldCurrentValue field = new PaneFieldCurrentValue();
        field.label        = label;
        field.value        = point;
        field.unitsFactors = unitsFactors;
        field.suffix       = suffix;
        return field;
    }
}
