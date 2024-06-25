/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.message;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.message.UserMessageDeviceRecord;
import com.optio3.cloud.hub.persistence.message.UserMessageRecord;
import com.optio3.cloud.model.TypedRecordIdentity;

@JsonTypeName("UserMessageDevice")
public class UserMessageDevice extends UserMessage
{
    @Optio3MapAsReadOnly
    public TypedRecordIdentity<DeviceRecord> device;

    //--//

    @Override
    public UserMessageRecord newRecord()
    {
        return new UserMessageDeviceRecord();
    }
}
