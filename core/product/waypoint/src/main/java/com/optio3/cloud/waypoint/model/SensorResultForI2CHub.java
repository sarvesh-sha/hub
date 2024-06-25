/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.waypoint.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;

@JsonTypeName("SensorResultForI2CHub")
public class SensorResultForI2CHub extends SensorResult
{
    public static class Scan
    {
        public int    bus;
        public int    address;
        public String device;
    }

    public boolean    present;
    public List<Scan> busScan = Lists.newArrayList();
}
