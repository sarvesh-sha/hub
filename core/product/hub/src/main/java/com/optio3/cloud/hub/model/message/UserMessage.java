/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.message;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.hub.persistence.message.UserMessageRecord;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.model.TypedRecordIdentity;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes({ @JsonSubTypes.Type(value = UserMessageAlert.class),
                @JsonSubTypes.Type(value = UserMessageDevice.class),
                @JsonSubTypes.Type(value = UserMessageGeneric.class),
                @JsonSubTypes.Type(value = UserMessageRoleManagement.class),
                @JsonSubTypes.Type(value = UserMessageReport.class),
                @JsonSubTypes.Type(value = UserMessageWorkflow.class) })
public abstract class UserMessage extends BaseModel
{
    @Optio3MapAsReadOnly
    public TypedRecordIdentity<UserRecord> user;

    @Optio3MapAsReadOnly
    public String subject;

    @Optio3MapAsReadOnly
    public String body;

    public boolean flagNew;
    public boolean flagRead;
    public boolean flagActive;

    //--//

    public abstract UserMessageRecord newRecord();
}
