/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.digineous;

import java.time.ZonedDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.protocol.model.BaseObjectModel;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModel;
import com.optio3.protocol.model.WellKnownEquipmentClassOrCustom;
import com.optio3.protocol.model.WellKnownPointClassOrCustom;
import com.optio3.protocol.model.WellKnownTag;
import com.optio3.protocol.model.ipn.IpnObjectModel;
import com.optio3.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

@JsonSubTypes({ @JsonSubTypes.Type(value = Digineous_AnalogSignal.class), @JsonSubTypes.Type(value = Digineous_LogSignal.class), @JsonSubTypes.Type(value = Digineous_StatusSignal.class) })
public abstract class BaseDigineousModel extends IpnObjectModel
{
    private static final String PREFIX = "DIGI";

    public String                          machineId;
    public String                          machineName;
    public WellKnownEquipmentClassOrCustom machineEquipmentClass;

    public WellKnownEquipmentClassOrCustom deviceEquipmentClass;

    @JsonIgnore
    public DigineousDeviceFlavor deviceFlavor;

    @JsonIgnore
    public int deviceId;

    public WellKnownPointClassOrCustom pointClass;
    public List<String>                tags;
    public String                      description;
    public EngineeringUnits            units;

    //--//

    @Override
    protected BaseObjectModel createEmptyCopy()
    {
        BaseDigineousModel obj = (BaseDigineousModel) super.createEmptyCopy();

        obj.machineId             = machineId;
        obj.machineName           = machineName;
        obj.machineEquipmentClass = machineEquipmentClass;

        obj.deviceEquipmentClass = deviceEquipmentClass;
        obj.deviceFlavor         = deviceFlavor;
        obj.deviceId             = deviceId;

        obj.pointClass  = pointClass;
        obj.tags        = tags;
        obj.description = description;
        obj.units       = units;

        return obj;
    }

    @Override
    public boolean shouldCommitReachabilityChange(boolean isReachable,
                                                  ZonedDateTime lastReachable)
    {
        return false;
    }

    @Override
    public void fillClassificationDetails(ClassificationDetails detailsForGroup,
                                          ClassificationDetails detailsForParent,
                                          ClassificationDetails detailsForPoint)
    {
        if (deviceFlavor != null)
        {
            detailsForGroup.noEquipmentClassInDisplayName = true;
            detailsForGroup.equipmentName                 = machineName;
            detailsForGroup.equipmentClass                = machineEquipmentClass;
            detailsForGroup.instanceSelector              = machineId;

            detailsForParent.equipmentClass   = deviceEquipmentClass;
            detailsForParent.pointClass       = pointClass;
            detailsForParent.instanceSelector = Integer.toString(deviceId);

            detailsForPoint.pointClass = pointClass;

            for (String tag : CollectionUtils.asEmptyCollectionIfNull(tags))
            {
                detailsForPoint.addExtraTag(tag, false);
            }

            switch (deviceFlavor)
            {
                case BlackBox:
                    break;

                case InfiniteImpulse_Min:
                    detailsForParent.addExtraTag(WellKnownTag.Minimum, true);
                    break;

                case InfiniteImpulse_Avg:
                    detailsForParent.addExtraTag(WellKnownTag.Average, true);
                    break;

                case InfiniteImpulse_Max:
                    detailsForParent.addExtraTag(WellKnownTag.Maximum, true);
                    break;
            }
        }
    }

    @Override
    public boolean parseId(String id)
    {
        if (StringUtils.startsWith(id, PREFIX))
        {
            try
            {
                String[] parts = StringUtils.split(id, "::");
                if (parts.length == 3 && PREFIX.equals(parts[0]))
                {
                    deviceFlavor = DigineousDeviceFlavor.valueOf(parts[1]);
                    deviceId     = Integer.parseInt(parts[2]);
                    return true;
                }
            }
            catch (Exception e)
            {
                // Not a valid id.
            }
        }

        return false;
    }

    public static String buildId(DigineousDeviceFlavor flavor,
                                 int deviceId)
    {
        return String.format("%s::%s::%d", PREFIX, flavor, deviceId);
    }

    //--//

    @Override
    public boolean overrideDescriptorsPerObject()
    {
        return true;
    }

    @Override
    public String overrideDescription(FieldModel model,
                                      String description)
    {
        return this.description;
    }

    @Override
    public EngineeringUnits overrideUnits(FieldModel model,
                                          EngineeringUnits units)
    {
        return this.units;
    }

    @Override
    public WellKnownPointClassOrCustom overridePointClass(FieldModel model,
                                                          WellKnownPointClassOrCustom pointClass)
    {
        return this.pointClass;
    }
}
