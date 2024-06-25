/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.epsolar;

import java.lang.reflect.Field;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.google.common.collect.Maps;
import com.optio3.protocol.model.BaseObjectModel;
import com.optio3.protocol.model.WellKnownEquipmentClass;
import com.optio3.protocol.model.ipn.IpnObjectModel;
import com.optio3.serialization.Reflection;
import org.apache.commons.lang3.StringUtils;

@JsonSubTypes({ @JsonSubTypes.Type(value = EpSolar_LoadParameters.class), @JsonSubTypes.Type(value = EpSolar_RealTimeData.class), @JsonSubTypes.Type(value = EpSolar_BatteryParameters.class) })
public abstract class BaseEpSolarModel extends IpnObjectModel
{
    private static final ConcurrentMap<Class<?>, Map<String, EpSolarFieldModel>> s_classToDescriptors = Maps.newConcurrentMap();

    public static Map<String, EpSolarFieldModel> collectRegisters(Class<?> clz)
    {
        Map<String, EpSolarFieldModel> res = s_classToDescriptors.get(clz);
        if (res != null)
        {
            return res;
        }

        TreeMap<String, EpSolarFieldModel> newMap = null;

        Map<String, Field> fields = Reflection.collectFields(clz);
        for (Field f : fields.values())
        {
            EpSolarField t = f.getAnnotation(EpSolarField.class);
            if (t != null)
            {
                if (newMap == null)
                {
                    newMap = Maps.newTreeMap();
                }

                EpSolarFieldModel model = new EpSolarFieldModel(f.getGenericType(), t.pdu(), t.length(), t.signed(), t.writable(), t.fixedScaling());

                newMap.put(f.getName(), model);
            }
        }

        if (newMap == null)
        {
            res = Collections.emptyMap();
        }
        else
        {
            res = Collections.unmodifiableMap(newMap);
        }

        Map<String, EpSolarFieldModel> oldRes = s_classToDescriptors.putIfAbsent(clz, res);
        return oldRes != null ? oldRes : res;
    }

    //--//

    @JsonIgnore
    public int unitId;

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
        BaseEpSolarModel copy = (BaseEpSolarModel) super.createEmptyCopy();
        copy.unitId = unitId;
        return copy;
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
        detailsForParent.equipmentClass = WellKnownEquipmentClass.ChargeController.asWrapped();
    }

    public void preProcess()
    {
    }

    public void postProcess()
    {
    }
}
