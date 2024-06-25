/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.normalizations;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.cloud.hub.persistence.location.LocationRecord;
import com.optio3.cloud.model.TypedRecordIdentity;

public class ValidationEquipmentRule
{
    public String name;

    public String equipmentClassId;

    public List<String> locationNames = Lists.newArrayList();

    public TypedRecordIdentity<LocationRecord> location;

    public int minNumber;

    public int maxNumber;

    public List<ValidationEquipmentRulePointClassCriteria> points = Lists.newArrayList();
}
