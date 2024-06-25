/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model;

import java.util.Collection;
import java.util.Map;

import com.google.common.collect.Maps;

public class MetadataAggregation
{
    public Map<String, Collection<String>> buildingEquipments = Maps.newHashMap();
    public Map<String, String>             equipmentNames     = Maps.newHashMap();
    public Map<String, String>             equipmentClassIds  = Maps.newHashMap();
    public Map<String, String>             controllerNames    = Maps.newHashMap();
}
