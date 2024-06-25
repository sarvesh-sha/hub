/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.event;

import java.time.ZonedDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.model.alert.AlertFilterRequest;
import com.optio3.cloud.hub.model.audit.AuditFilterRequest;
import com.optio3.cloud.hub.model.workflow.WorkflowFilterRequest;
import com.optio3.cloud.model.BasePaginatedRequest;
import com.optio3.cloud.model.SortCriteria;
import com.optio3.util.CollectionUtils;

@JsonTypeName("EventFilterRequest")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes({ @JsonSubTypes.Type(value = AlertFilterRequest.class), @JsonSubTypes.Type(value = AuditFilterRequest.class), @JsonSubTypes.Type(value = WorkflowFilterRequest.class) })
public class EventFilterRequest extends BasePaginatedRequest
{
    public List<String> assetIDs;
    public List<String> locationIDs;
    public boolean      locationInclusive;

    public String likeDeviceManufacturerName;
    public String likeDeviceProductName;
    public String likeDeviceModelName;

    public List<SortCriteria> sortBy;

    public boolean       evaluateUpdatedOn;
    public ZonedDateTime rangeStart;
    public ZonedDateTime rangeEnd;

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setDeviceCategoryIDs(List<String> deviceCategoryIDs)
    {
    }

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setDeviceManufacturerIDs(List<String> deviceManufacturerIDs)
    {
    }

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setDeviceProductNames(List<String> deviceProductNames)
    {
    }

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setDeviceModelNumbers(List<String> deviceModelNumbers)
    {
    }

    //--//

    public boolean hasAssets()
    {
        return hasItems(assetIDs);
    }

    public boolean hasLocations()
    {
        return hasItems(locationIDs);
    }

    public boolean hasSorting()
    {
        return hasItems(sortBy);
    }

    protected static boolean hasItems(List<?> coll)
    {
        return CollectionUtils.isNotEmpty(coll);
    }
}
