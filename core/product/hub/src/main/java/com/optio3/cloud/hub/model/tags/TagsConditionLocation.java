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

@JsonTypeName("TagsConditionLocation")
public class TagsConditionLocation extends TagsCondition
{
    public String locationSysId;

    //--//

    public static TagsConditionLocation build(String sysId)
    {
        TagsConditionLocation query = new TagsConditionLocation();
        query.locationSysId = sysId;
        return query;
    }

    //--//

    @Override
    public void validate(StringBuilder path)
    {
        if (locationSysId == null)
        {
            throw validationFailure(path, "locationSysId");
        }
    }

    @Override
    public BitSet evaluate(TagsQueryContext context)
    {
        return context.lookupTag(AssetRecord.WellKnownTags.sysLocation, locationSysId);
    }
}
