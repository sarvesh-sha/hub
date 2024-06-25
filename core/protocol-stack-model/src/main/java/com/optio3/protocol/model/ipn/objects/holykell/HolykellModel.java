/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.holykell;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.optio3.protocol.model.BaseObjectModel;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.WellKnownEquipmentClass;
import com.optio3.protocol.model.WellKnownPointClass;
import com.optio3.protocol.model.ipn.IpnObjectModel;
import org.apache.commons.lang3.StringUtils;

public class HolykellModel extends IpnObjectModel
{
    @JsonIgnore
    public int unitId;

    @FieldModelDescription(description = "Liquid Level", units = EngineeringUnits.meters, pointClass = WellKnownPointClass.HolykellLevel, debounceSeconds = 10, minimumDelta = 0.03)
    public float level;

    @FieldModelDescription(description = "Sensor Temperature", units = EngineeringUnits.degrees_celsius, pointClass = WellKnownPointClass.HolykellTemperature, debounceSeconds = 10, minimumDelta = 0.5)
    public float temperature;

    //--//

    @Override
    public boolean shouldCommitReachabilityChange(boolean isReachable,
                                                  ZonedDateTime lastReachable)
    {
        return true;
    }

    @Override
    protected BaseObjectModel createEmptyCopy()
    {
        HolykellModel copy = (HolykellModel) super.createEmptyCopy();
        copy.unitId = unitId;
        return copy;
    }

    @Override
    public String extractBaseId()
    {
        return "Holykell";
    }

    @Override
    public String extractUnitId()
    {
        return Integer.toString(unitId);
    }

    @Override
    public boolean parseId(String id)
    {
        final String baseId = extractBaseId();
        if (StringUtils.startsWith(id, baseId))
        {
            String[] parts = StringUtils.split(id, '/');
            if (parts.length == 2 && StringUtils.equals(baseId, parts[0]))
            {
                try
                {
                    unitId = Integer.parseInt(parts[1]);
                    return true;
                }
                catch (NumberFormatException e)
                {
                    // Not a valid id.
                }
            }
        }

        return false;
    }

    @Override
    public void fillClassificationDetails(ClassificationDetails detailsForGroup,
                                          ClassificationDetails detailsForParent,
                                          ClassificationDetails detailsForPoint)
    {
        detailsForParent.equipmentClass   = WellKnownEquipmentClass.LevelSensor.asWrapped();
        detailsForParent.instanceSelector = Integer.toString(unitId);
    }
}
