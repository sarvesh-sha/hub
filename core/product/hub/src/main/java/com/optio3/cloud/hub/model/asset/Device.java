/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.asset;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;

@JsonTypeName("Device")
@JsonSubTypes({ @JsonSubTypes.Type(value = BACnetDevice.class), @JsonSubTypes.Type(value = IpnDevice.class) })
public class Device extends Asset
{
    public String manufacturerName;
    public String productName;
    public String modelName;
    public String firmwareVersion;

    public int minutesBeforeTransitionToUnreachable;
    public int minutesBeforeTransitionToReachable;

    @Optio3MapAsReadOnly
    public boolean reachable;

    //--//

    @Override
    public AssetRecord newRecord()
    {
        return new DeviceRecord();
    }
}
