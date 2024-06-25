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

@JsonTypeName("TagsConditionEquipmentClass")
public class TagsConditionEquipmentClass extends TagsCondition
{
    public String equipmentClass;

    //--//

    public static TagsConditionEquipmentClass build(String equipmentClass)
    {
        TagsConditionEquipmentClass query = new TagsConditionEquipmentClass();
        query.equipmentClass = equipmentClass;
        return query;
    }

    //--//

    @Override
    public void validate(StringBuilder path)
    {
        if (equipmentClass == null)
        {
            throw validationFailure(path, "equipmentClass");
        }
    }

    @Override
    public BitSet evaluate(TagsQueryContext context)
    {
        return context.lookupTag(AssetRecord.WellKnownTags.equipmentClassId, equipmentClass);
    }
}
