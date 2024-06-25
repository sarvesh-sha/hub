/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.asset;

import java.time.ZonedDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;
import com.optio3.cloud.hub.model.tags.TagsCondition;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.model.BasePaginatedRequest;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.model.SortCriteria;

@JsonTypeName("AssetFilterRequest")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes({ @JsonSubTypes.Type(value = DeviceElementFilterRequest.class),
                @JsonSubTypes.Type(value = DeviceFilterRequest.class),
                @JsonSubTypes.Type(value = GatewayFilterRequest.class),
                @JsonSubTypes.Type(value = HostFilterRequest.class),
                @JsonSubTypes.Type(value = LocationFilterRequest.class),
                @JsonSubTypes.Type(value = NetworkFilterRequest.class) })
public class AssetFilterRequest extends BasePaginatedRequest
{
    private final static AssetRelationship[] c_defaultRelationships = { AssetRelationship.structural };

    public List<String> locationIDs;
    public boolean      locationInclusive;
    public boolean      locationMissing;

    public List<String> sysIds;

    public List<String>            parentIDs;
    public List<AssetRelationship> parentRelations;
    public TagsCondition           parentTagsQuery;

    public String likeFilter;

    public List<String>            childrenIDs;
    public List<AssetRelationship> childrenRelations;

    public List<AssetState> stateIDs;

    public List<SortCriteria> sortBy;

    public ZonedDateTime discoveryRangeStart;
    public ZonedDateTime discoveryRangeEnd;

    public boolean       hasNoMetadata;
    public boolean       hasMetadata;
    public TagsCondition tagsQuery;

    public boolean isHidden;
    public boolean isNotHidden;

    //--//

    public AssetFilterResponse handlePagination(List<RecordIdentity> results)
    {
        AssetFilterResponse response = new AssetFilterResponse();
        response.handlePagination(this, results);
        return response;
    }

    public void addState(AssetState state)
    {
        if (stateIDs == null)
        {
            stateIDs = Lists.newArrayList();
        }

        stateIDs.add(state);
    }

    public void setParent(String sysId,
                          AssetRelationship... relationships)
    {
        if (relationships == null || relationships.length == 0)
        {
            relationships = c_defaultRelationships;
        }

        parentIDs = Lists.newArrayList();
        parentIDs.add(sysId);
        parentRelations = Lists.newArrayList(relationships);
    }

    public static AssetFilterRequest createFilterForParent(String sysId,
                                                           AssetRelationship... relationships)
    {
        AssetFilterRequest filters = new AssetFilterRequest();
        filters.setParent(sysId, relationships);

        return filters;
    }

    public static AssetFilterRequest createFilterForParent(AssetRecord rec,
                                                           AssetRelationship... relationships)
    {
        return createFilterForParent(rec.getSysId(), relationships);
    }

    public boolean forceLike()
    {
        return false;
    }
}
