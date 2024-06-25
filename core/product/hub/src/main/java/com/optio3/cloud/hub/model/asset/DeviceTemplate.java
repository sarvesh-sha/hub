/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.asset;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesPropertyType;
import com.optio3.protocol.model.ipn.IpnObjectModel;

public class DeviceTemplate
{
    public Map<String, TimeSeriesPropertyType> elements = Maps.newHashMap();

    //--//

    public static String extractPath(IpnObjectModel obj)
    {
        List<String> path = Lists.newArrayList();

        path.add(obj.extractBaseId());

        Class<?> clz = obj.getClass();
        while (true)
        {
            clz = clz.getSuperclass();
            if (clz == IpnObjectModel.class)
            {
                break;
            }

            path.add(0, clz.getSimpleName());
        }

        return String.join("/", path);
    }
}

