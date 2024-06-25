/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.identity;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3AutoTrim;

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

    public List<String> roles = Lists.newArrayList();
}
