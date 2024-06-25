/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.asset;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.persistence.asset.IpnDeviceRecord;

@JsonTypeName("IpnDevice")
public class IpnDevice extends Device
{
    @Override
    public IpnDeviceRecord newRecord()
    {
        return new IpnDeviceRecord();
    }
}
