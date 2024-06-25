/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.asset;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;

@JsonTypeName("DeviceElementFilterRequest")
public class DeviceElementFilterRequest extends AssetFilterRequest
{
    public boolean hasNoSampling;
    public boolean hasAnySampling;

    //--//

    public static DeviceElementFilterRequest createFilterForParent(String sysId,
                                                                   AssetRelationship... relationships)
    {
        DeviceElementFilterRequest filters = new DeviceElementFilterRequest();
        filters.setParent(sysId, relationships);

        return filters;
    }

    public static DeviceElementFilterRequest createFilterForParent(AssetRecord rec,
                                                                   AssetRelationship... relationships)
    {
        return createFilterForParent(rec.getSysId(), relationships);
    }
}
