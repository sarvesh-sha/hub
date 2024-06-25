/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.normalization;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.model.location.LocationType;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.util.CollectionUtils;

public class DeviceElementClassificationOverrides
{
    public List<NormalizationEquipment> equipments;
    public String                       pointName;
    public String                       pointClassId;

    public List<NormalizationEquipmentLocation> locationsWithType;

    // TODO: UPGRADE PATCH: Remove after normalization/classification has been run
    public void setLocations(List<String> locationNames)
    {
        HubApplication.reportPatchCall(locationNames);

        locationsWithType = CollectionUtils.transformToList(locationNames, (name) ->
        {
            NormalizationEquipmentLocation location = new NormalizationEquipmentLocation();
            location.name = name;
            location.type = LocationType.OTHER;
            return location;
        });
    }

    // TODO: UPGRADE PATCH: Remove after normalization/classification has been run
    public void setEquipment(String equipment)
    {
        HubApplication.reportPatchCall(equipment);

        NormalizationEquipment eq = ensureFirstEquipment();
        eq.name = equipment;
    }

    // TODO: UPGRADE PATCH: Remove after normalization/classification has been run
    public void setEquipmentClassId(String equipmentClassId)
    {
        HubApplication.reportPatchCall(equipmentClassId);

        NormalizationEquipment eq = ensureFirstEquipment();
        eq.equipmentClassId = equipmentClassId;
    }

    public static void cleanUp(SessionHolder sessionHolder,
                               Map<String, DeviceElementClassificationOverrides> overrides)
    {
        RecordHelper<DeviceElementRecord> helper = sessionHolder.createHelper(DeviceElementRecord.class);
        Iterator<String> it = overrides.keySet()
                                       .iterator();
        while (it.hasNext())
        {
            String              sysId      = it.next();
            DeviceElementRecord rec_object = helper.getOrNull(sysId);
            if (rec_object == null)
            {
                it.remove();
            }
        }
    }

    private NormalizationEquipment ensureFirstEquipment()
    {
        if (equipments == null)
        {
            equipments = Lists.newArrayList();
        }

        if (equipments.isEmpty())
        {
            NormalizationEquipment eq = new NormalizationEquipment();
            equipments.add(eq);
            return eq;
        }
        else
        {
            return CollectionUtils.firstElement(equipments);
        }
    }
}
