/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.admin;

import java.util.List;

import com.optio3.cloud.builder.persistence.customer.CustomerRecord;
import com.optio3.cloud.model.TypedRecordIdentity;

public class CustomerUpgradeLevel
{
    public TypedRecordIdentity<CustomerRecord> customer;
    public List<ServiceUpgradeLevel>           services;
}
