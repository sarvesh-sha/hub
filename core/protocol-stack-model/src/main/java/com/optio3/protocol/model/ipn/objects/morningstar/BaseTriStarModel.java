/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.morningstar;

import java.lang.reflect.Field;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentMap;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.google.common.collect.Maps;
import com.optio3.protocol.model.WellKnownEquipmentClass;
import com.optio3.protocol.model.ipn.IpnObjectModel;
import com.optio3.serialization.Reflection;

@JsonSubTypes({ @JsonSubTypes.Type(value = TriStar_Charger.class),
                @JsonSubTypes.Type(value = TriStar_EEPROM.class),
                @JsonSubTypes.Type(value = TriStar_FilteredADC.class),
                @JsonSubTypes.Type(value = TriStar_MPPT.class),
                @JsonSubTypes.Type(value = TriStar_Status.class),
                @JsonSubTypes.Type(value = TriStar_Temperatures.class) })
public abstract class BaseTriStarModel extends IpnObjectModel
{
    private static final ConcurrentMap<Class<?>, Map<String, TriStarFieldModel>> s_classToDescriptors = Maps.newConcurrentMap();

    public static Map<String, TriStarFieldModel> collectRegisters(Class<?> clz)
    {
        Map<String, TriStarFieldModel> res = s_classToDescriptors.get(clz);
        if (res != null)
        {
            return res;
        }

        TreeMap<String, TriStarFieldModel> newMap = null;

        Map<String, Field> fields = Reflection.collectFields(clz);
        for (Field f : fields.values())
        {
            TriStarField t = f.getAnnotation(TriStarField.class);
            if (t != null)
            {
                if (newMap == null)
                {
                    newMap = Maps.newTreeMap();
                }

                TriStarFieldModel model = new TriStarFieldModel(f.getGenericType(), t.pdu(), t.length(), t.signed(), t.fixedScaling(), t.voltageScaling(), t.currentScaling());

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

        Map<String, TriStarFieldModel> oldRes = s_classToDescriptors.putIfAbsent(clz, res);
        return oldRes != null ? oldRes : res;
    }

    //--//

    @Override
    public boolean shouldCommitReachabilityChange(boolean isReachable,
                                                  ZonedDateTime lastReachable)
    {
        return true;
    }

    @Override
    public void fillClassificationDetails(ClassificationDetails detailsForGroup,
                                          ClassificationDetails detailsForParent,
                                          ClassificationDetails detailsForPoint)
    {
        detailsForParent.equipmentClass = WellKnownEquipmentClass.ChargeController.asWrapped();
    }
}
