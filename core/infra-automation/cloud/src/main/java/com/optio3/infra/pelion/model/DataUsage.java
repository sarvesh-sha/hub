/*
 * Copyright (C) 2017-2020, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.pelion.model;

import java.time.ZonedDateTime;
import java.util.Map;

import com.google.common.collect.Maps;

public class DataUsage
{
    public Map<ZonedDateTime, Integer> mobileTerminated = Maps.newHashMap();

    public Map<ZonedDateTime, Integer> mobileOriginated = Maps.newHashMap();
}
