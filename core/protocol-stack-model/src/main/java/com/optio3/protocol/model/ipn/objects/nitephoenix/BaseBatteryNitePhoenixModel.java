/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.nitephoenix;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.protocol.model.BaseObjectModel;
import com.optio3.protocol.model.WellKnownEquipmentClass;
import com.optio3.protocol.model.can.CanExtendedMessageType;
import com.optio3.protocol.model.ipn.IpnObjectsState;
import org.apache.commons.lang3.StringUtils;

@JsonSubTypes({ @JsonSubTypes.Type(value = NitePhoenix_BatteryMonitoringSystem_AlgorithmOutputs.class),
                @JsonSubTypes.Type(value = NitePhoenix_BatteryMonitoringSystem_AuxiliaryBatteryParameters.class),
                @JsonSubTypes.Type(value = NitePhoenix_BatteryMonitoringSystem_StartingBatteryParameters.class) })
public abstract class BaseBatteryNitePhoenixModel extends BaseNitePhoenixModel
{
    @JsonIgnore
    public int unitId;

    //--//

    @Override
    protected BaseObjectModel createEmptyCopy()
    {
        BaseBatteryNitePhoenixModel copy = (BaseBatteryNitePhoenixModel) super.createEmptyCopy();
        copy.unitId = unitId;
        return copy;
    }

    @Override
    public String extractUnitId()
    {
        if (unitId > 0)
        {
            return Integer.toString(unitId);
        }

        return super.extractUnitId();
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
    public void initializeFromAnnotation(CanExtendedMessageType annoExt)
    {
        switch (annoExt.sourceAddress())
        {
            case 0x48:
                unitId = 1;
                break;

            case 0x49:
                unitId = 2;
                break;

            case 0x4A:
                unitId = 3;
                break;
        }
    }

    @Override
    public void postProcess(IpnObjectsState state,
                            BaseNitePhoenixModel previousValue)
    {
        if (unitId > 0)
        {
            return;
        }

        super.postProcess(state, previousValue);
    }

    @Override
    public void fillClassificationDetails(ClassificationDetails detailsForGroup,
                                          ClassificationDetails detailsForParent,
                                          ClassificationDetails detailsForPoint)
    {
        if (unitId > 0)
        {
            detailsForParent.equipmentClass   = WellKnownEquipmentClass.Sensor.asWrapped();
            detailsForParent.instanceSelector = String.format("Position %d", unitId);
            detailsForPoint.addExtraTag(String.format("Position%d", unitId),false);
            return;
        }

        super.fillClassificationDetails(detailsForGroup, detailsForParent, detailsForPoint);
    }
}
