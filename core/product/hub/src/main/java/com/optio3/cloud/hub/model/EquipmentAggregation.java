/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.cloud.hub.logic.normalizations.EquipmentClass;
import com.optio3.cloud.hub.logic.normalizations.LocationClass;
import com.optio3.cloud.hub.logic.normalizations.PointClass;
import com.optio3.cloud.hub.model.location.LocationHierarchy;

public class EquipmentAggregation
{
    public List<EquipmentHierarchy> equipments  = Lists.newArrayList();
    public List<String>             controllers = Lists.newArrayList();

    public List<LocationHierarchy> locationHierarchy;
    public List<PointClass>        pointClasses;
    public List<EquipmentClass>    equipmentClasses;
    public List<LocationClass>     locationClasses;
}
