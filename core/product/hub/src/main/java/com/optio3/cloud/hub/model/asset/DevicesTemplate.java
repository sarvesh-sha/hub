/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.asset;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesPropertyType;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.protocol.model.ipn.IpnObjectModel;
import com.optio3.serialization.Reflection;

public class DevicesTemplate
{
    public Map<String, DeviceTemplate> devices = Maps.newHashMap();

    //--//

    public void collect()
    {
        Set<Class<? extends IpnObjectModel>> subTypes = Reflection.collectJsonSubTypes(IpnObjectModel.class);

        for (Class<? extends IpnObjectModel> subType : subTypes)
        {
            Map<String, TimeSeriesPropertyType> elements = AssetRecord.PropertyTypeExtractor.classifyTemplate(subType, true);

            if (!elements.isEmpty())
            {
                DeviceTemplate dt = new DeviceTemplate();
                dt.elements = elements;

                IpnObjectModel obj = Reflection.newInstance(subType);
                devices.put(DeviceTemplate.extractPath(obj), dt);
            }
        }
    }
}

