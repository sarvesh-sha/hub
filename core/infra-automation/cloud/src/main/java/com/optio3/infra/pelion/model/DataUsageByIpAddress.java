/*
 * Copyright (C) 2017-2020, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.pelion.model;

import java.util.Map;

import com.google.common.collect.Maps;

public class DataUsageByIpAddress
{
    public static class Details
    {
        public int            bytes;
        public PelionDateTime lastSeen;
    }

    public Map<String, Details> dataUsageByIp = Maps.newHashMap();

    public int totalData;
}
