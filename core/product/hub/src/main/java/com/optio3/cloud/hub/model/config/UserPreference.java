/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.config;

import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.model.TypedRecordIdentity;

public class UserPreference extends BaseModel
{
    @Optio3MapAsReadOnly
    public TypedRecordIdentity<UserRecord> user;

    public String path;

    public String name;

    public String value;
}
