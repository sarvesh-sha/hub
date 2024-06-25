/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.customer;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.builder.persistence.customer.CustomerRecord;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.model.TypedRecordIdentity;

public class CustomerSharedUser extends BaseModel
{
    @Optio3MapAsReadOnly
    public TypedRecordIdentity<CustomerRecord> customer;

    //--//

    public String firstName;

    public String lastName;

    public String emailAddress;

    public String phoneNumber;

    public String password;

    public List<String> roles = Lists.newArrayList();
}
