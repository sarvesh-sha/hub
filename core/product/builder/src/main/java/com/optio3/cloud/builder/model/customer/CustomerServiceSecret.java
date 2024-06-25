/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.customer;

import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.model.TypedRecordIdentity;

public class CustomerServiceSecret extends BaseModel
{
    @Optio3MapAsReadOnly
    public TypedRecordIdentity<CustomerServiceRecord> service;

    //--//

    public String context;

    public String key;

    public String value;
}
