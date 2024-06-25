/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.message;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.persistence.message.UserMessageGenericRecord;
import com.optio3.cloud.hub.persistence.message.UserMessageRecord;

@JsonTypeName("UserMessageGeneric")
public class UserMessageGeneric extends UserMessage
{
    @Override
    public UserMessageRecord newRecord()
    {
        return new UserMessageGenericRecord();
    }
}
