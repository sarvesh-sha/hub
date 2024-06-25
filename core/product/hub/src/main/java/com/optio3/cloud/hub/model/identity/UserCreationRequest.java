/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.identity;

import com.optio3.cloud.annotation.Optio3AutoTrim;
import com.optio3.cloud.hub.persistence.config.RoleRecord;
import com.optio3.cloud.model.TypedRecordIdentityList;

public class UserCreationRequest
{
    @Optio3AutoTrim()
    public String firstName;

    @Optio3AutoTrim()
    public String lastName;

    @Optio3AutoTrim()
    public String emailAddress;

    @Optio3AutoTrim()
    public String phoneNumber;

    @Optio3AutoTrim()
    public String password;

    public TypedRecordIdentityList<RoleRecord> roles = new TypedRecordIdentityList<>();
}
