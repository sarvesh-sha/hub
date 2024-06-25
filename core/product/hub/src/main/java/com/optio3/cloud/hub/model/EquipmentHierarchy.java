/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;

public class EquipmentHierarchy
{
    public String             sysId;
    public String             name;
    public String             equipmentClassId;
    public String             locationSysId;
    public Collection<String> tags;

    public final List<EquipmentHierarchy> children = Lists.newArrayList();
}
