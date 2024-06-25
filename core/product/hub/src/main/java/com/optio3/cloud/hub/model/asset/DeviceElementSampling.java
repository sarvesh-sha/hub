/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.asset;

import java.util.List;

public class DeviceElementSampling
{
    public static final String COLLECT_DEFAULTS = "<defaults>";

    public String propertyName;
    public int    samplingPeriod;

    //--//

    public static void add(List<DeviceElementSampling> list,
                           String propertyName,
                           int samplingPeriod)
    {
        DeviceElementSampling cfg = new DeviceElementSampling();
        cfg.propertyName   = propertyName;
        cfg.samplingPeriod = samplingPeriod;
        list.add(cfg);
    }

    public static <E extends Enum<E>> void add(List<DeviceElementSampling> list,
                                               Enum<E> property,
                                               int samplingPeriod)
    {
        add(list, property.name(), samplingPeriod);
    }
}
