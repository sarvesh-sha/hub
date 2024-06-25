/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.customization.digineous.model;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3AutoTrim;
import com.optio3.cloud.annotation.Optio3IncludeInApiDefinitions;
import com.optio3.protocol.model.WellKnownEquipmentClass;
import com.optio3.protocol.model.WellKnownEquipmentClassOrCustom;
import com.optio3.protocol.model.ipn.objects.digineous.DigineousDeviceFlavor;

@Optio3IncludeInApiDefinitions
public class DigineousDeviceLibrary
{
    @Optio3AutoTrim()
    public String id;

    @Optio3AutoTrim()
    public String name;

    //--//

    public DigineousDeviceFlavor deviceFlavor;

    public WellKnownEquipmentClassOrCustom equipmentClass;

    public final List<DigineousPointLibrary> points = Lists.newArrayList();

    public DigineousPointLibrary locatePoint(String identifier)
    {
        for (DigineousPointLibrary point : points)
        {
            if (point.identifier.equals(identifier))
            {
                return point;
            }
        }

        return null;
    }

    public void ensureInitialized()
    {
        if (deviceFlavor == null)
        {
            deviceFlavor = DigineousDeviceFlavor.BlackBox;
        }

        if (equipmentClass == null)
        {
            switch (deviceFlavor)
            {
                case BlackBox:
                    equipmentClass = WellKnownEquipmentClass.Sensor.asWrapped();
                    break;

                case InfiniteImpulse_Min:
                case InfiniteImpulse_Avg:
                case InfiniteImpulse_Max:
                    equipmentClass = WellKnownEquipmentClass.Vibration.asWrapped();
                    break;
            }
        }
    }
}
