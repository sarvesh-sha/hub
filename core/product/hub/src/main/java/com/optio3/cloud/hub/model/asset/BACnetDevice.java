/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.asset;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.persistence.asset.BACnetDeviceRecord;

@JsonTypeName("BACnetDevice")
public class BACnetDevice extends Device
{
    @Override
    public BACnetDeviceRecord newRecord()
    {
        return new BACnetDeviceRecord();
    }
}
