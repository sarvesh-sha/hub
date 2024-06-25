/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.asset;

import java.util.Map;

import com.google.common.collect.Maps;

public class DevicesSamplingTemplate
{
    public Map<String, DeviceSamplingTemplate> devices = Maps.newHashMap();

    //--//

    public Integer lookup(String deviceId,
                          String elementId)
    {
        if (devices != null)
        {
            DeviceSamplingTemplate device = devices.get(deviceId);
            if (device != null && device.elements != null)
            {
                return device.elements.get(elementId);
            }
        }

        return null;
    }
}

