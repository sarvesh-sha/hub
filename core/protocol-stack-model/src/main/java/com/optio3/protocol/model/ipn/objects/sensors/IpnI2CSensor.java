/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.sensors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.protocol.model.BaseObjectModel;
import com.optio3.protocol.model.WellKnownEquipmentClassOrCustom;
import com.optio3.protocol.model.ipn.IpnObjectModel;
import org.apache.commons.lang3.StringUtils;

@JsonSubTypes(
        { @JsonSubTypes.Type(value = IpnCurrent.class), @JsonSubTypes.Type(value = IpnHumidity.class), @JsonSubTypes.Type(value = IpnTemperature.class), @JsonSubTypes.Type(value = IpnVoltage.class) })
public abstract class IpnI2CSensor extends IpnObjectModel
{
    @JsonIgnore
    public int bus;

    @JsonIgnore
    public int channel;

    public WellKnownEquipmentClassOrCustom equipmentClass;
    public String                          instanceSelector;

    //--//

    @Override
    public void fillClassificationDetails(ClassificationDetails detailsForGroup,
                                          ClassificationDetails detailsForParent,
                                          ClassificationDetails detailsForPoint)
    {
        detailsForParent.equipmentClass   = equipmentClass;
        detailsForParent.instanceSelector = instanceSelector;
        detailsForParent.addExtraTag(instanceSelector,false);
    }

    //--//

    @Override
    protected BaseObjectModel createEmptyCopy()
    {
        IpnI2CSensor copy = (IpnI2CSensor) super.createEmptyCopy();
        copy.bus              = bus;
        copy.channel          = channel;
        copy.equipmentClass   = equipmentClass;
        copy.instanceSelector = instanceSelector;
        return copy;
    }

    @Override
    public String extractUnitId()
    {
        return String.format("%d#%d", bus, channel);
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
                    String[] parts2 = StringUtils.split(parts[1], '#');
                    bus     = Integer.parseInt(parts2[0]);
                    channel = Integer.parseInt(parts2[1]);
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
}
