/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.customer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.optio3.cloud.annotation.Optio3DontMap;
import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerSharedSecretRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerSharedUserRecord;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.model.TypedRecordIdentityList;

public class Customer extends BaseModel
{
    @Optio3MapAsReadOnly
    public String cloudId;

    public String name;

    //--//

    @Optio3MapAsReadOnly
    public TypedRecordIdentityList<CustomerServiceRecord> services = new TypedRecordIdentityList<>();

    @Optio3MapAsReadOnly
    public TypedRecordIdentityList<CustomerSharedUserRecord> sharedUsers = new TypedRecordIdentityList<>();

    @Optio3MapAsReadOnly
    public TypedRecordIdentityList<CustomerSharedSecretRecord> sharedSecrets = new TypedRecordIdentityList<>();

    //--//

    @Optio3DontMap
    @JsonIgnore
    public CustomerService[] rawServices;
}
