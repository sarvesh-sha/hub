/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.identity;

import com.optio3.cloud.annotation.Optio3AutoTrim;
import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.hub.persistence.config.RoleRecord;
import com.optio3.cloud.hub.persistence.config.UserGroupRecord;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.model.TypedRecordIdentityList;

public class User extends BaseModel
{
    @Optio3AutoTrim()
    public String firstName;

    @Optio3AutoTrim()
    public String lastName;

    @Optio3MapAsReadOnly
    public String emailAddress;

    public String phoneNumber;

    @Optio3MapAsReadOnly
    public TypedRecordIdentityList<RoleRecord> roles = new TypedRecordIdentityList<>();

    @Optio3MapAsReadOnly
    public TypedRecordIdentityList<UserGroupRecord> groups = new TypedRecordIdentityList<>();

    @Optio3MapAsReadOnly
    public String origin;
}
