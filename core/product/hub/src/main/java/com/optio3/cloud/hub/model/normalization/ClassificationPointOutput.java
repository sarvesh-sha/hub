/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.normalization;

import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.optio3.cloud.hub.logic.normalizations.NormalizationMatchHistory;

public class ClassificationPointOutput extends ClassificationPointInput
{
    public String                          normalizedName;
    public String                          oldNormalizedName;
    public List<NormalizationMatchHistory> normalizationHistory;

    public ClassificationPointOutputDetails lastResult;
    public ClassificationPointOutputDetails currentResult;

    public TreeMap<String, NormalizationEquipment> equipments             = new TreeMap<>();
    public TreeMap<String, Set<String>>            equipmentRelationships = new TreeMap<>();

    public List<NormalizationEquipmentLocation> locations = Lists.newArrayList();

    public Set<String> matchingDimensions = Sets.newHashSet();

    public Set<String> normalizationTags;
}
