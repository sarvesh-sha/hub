/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.normalization;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.cloud.hub.model.workflow.WorkflowOverrides;
import com.optio3.cloud.model.scheduler.BaseBackgroundActivityProgress;

public class DeviceElementNormalizationProgress extends BaseBackgroundActivityProgress
{
    public int devicesToProcess;
    public int devicesProcessed;
    public int elementsProcessed;

    public TreeMap<String, Integer> allWords        = new TreeMap<>();
    public TreeMap<String, Integer> allUnknownWords = new TreeMap<>();

    public Map<String, NormalizationEquipment> equipments             = Maps.newHashMap();
    public Map<String, Set<String>>            equipmentRelationships = Maps.newHashMap();

    public List<ClassificationPointOutput> details = Lists.newArrayList();

    public WorkflowOverrides workflowOverrides;
}

