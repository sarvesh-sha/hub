/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.asset;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("DeviceFilterRequest")
public class DeviceFilterRequest extends AssetFilterRequest
{
    public String likeDeviceManufacturerName;
    public String likeDeviceProductName;
    public String likeDeviceModelName;

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setDeviceUnclassified(boolean deviceUnclassified)
    {
    }

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

    @Override
    public boolean forceLike()
    {
        return true;
    }
}
