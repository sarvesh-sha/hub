/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.tags;

import java.util.BitSet;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.logic.tags.TagsQueryContext;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.util.BitSets;

@JsonTypeName("TagsConditionIsClassified")
public class TagsConditionIsClassified extends TagsCondition
{
    @Override
    public void validate(StringBuilder path)
    {
        // Nothing to check.
    }

    @Override
    public BitSet evaluate(TagsQueryContext context)
    {
        BitSet hasPointClass     = context.lookupTag(AssetRecord.WellKnownTags.pointClassId);
        BitSet hasEquipmentClass = context.lookupTag(AssetRecord.WellKnownTags.equipmentClassId);
        return BitSets.or(hasPointClass, hasEquipmentClass);
    }
}
