/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.visualization;

import com.optio3.cloud.annotation.Optio3SanitizeRecordReference;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.model.asset.graph.AssetGraphBinding;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.EngineeringUnitsFactors;
import com.optio3.protocol.model.EngineeringUnitsFamily;
import com.optio3.util.BoxingUtils;

public class TimeSeriesSourceConfiguration
{
    public String uuid;

    @Optio3SanitizeRecordReference(entityClass = DeviceElementRecord.class)
    public String id;

    public  String                      dimension;
    public  String                      color;
    public  double                      showMovingAverage;
    public  boolean                     onlyShowMovingAverage;
    public  boolean                     showDecimation;
    private TimeSeriesDecimationDisplay m_decimationDisplay;
    public  int                         axis;
    public  int                         panel;
    public  ToggleableNumericRange      range;
    public  TimeDuration                timeOffset;
    public  AssetGraphBinding           pointBinding;

    //--//

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setDescription(String description)
    {
    }

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setName(String name)
    {
    }

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setUnitsFactors(EngineeringUnitsFactors units)
    {
    }

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setUnit(EngineeringUnits unit)
    {
    }

    // TODO: UPGRADE PATCH: Legacy fixup for field type change.
    public void setFamily(EngineeringUnitsFamily family)
    {
        HubApplication.reportPatchCall(family);
    }

    public TimeSeriesDecimationDisplay getDecimationDisplay()
    {
        return BoxingUtils.get(m_decimationDisplay, TimeSeriesDecimationDisplay.Average);
    }

    public void setDecimationDisplay(TimeSeriesDecimationDisplay display)
    {
        m_decimationDisplay = display;
    }

    // TODO: UPGRADE PATCH: Legacy fixup for deleted field.
    public void setPeriodOffset(int periodOffset)
    {
        HubApplication.reportPatchCall(periodOffset);
    }
}
